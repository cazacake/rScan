package v1.a1;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class redditWebSocketServer extends WebSocketServer {

    public redditWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {				//onconnection
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }
	private TreeMap <String,Userdata> userDataList=new TreeMap<String,Userdata>();				//list of userdata

    @Override
    public void onMessage(WebSocket conn, String message) {												//when a message is recieved
        System.out.println("received message from " + conn.getRemoteSocketAddress() + ": " + message);	//display the address
        try {
        	ObjectMapper mapper=new ObjectMapper();
			Map <String,Object>result = mapper.readValue(
				    message, TreeMap.class);
			String username=(String) result.get("username");
			boolean doDefault=(Boolean)result.get("doDefault");
			String linkString="";
			if(username.startsWith("http://www.reddit.com/user/")){
				linkString=username;
			}else{
				linkString="http://www.reddit.com/user/"+username;
			}
			LinkedHashMap validityCheck=new LinkedHashMap();
			validityCheck.put("event","changeOutputHeader");
			conn.send(mapper.writeValueAsString(validityCheck));
			switch(checkUserValidity(linkString)){	
			case 404:
				LinkedHashMap s=new LinkedHashMap();
				s.put("event", "errorMessage");
				s.put("value", "not a valid username");
				String messageToSend=mapper.writeValueAsString(s);
				System.out.println(messageToSend);
				conn.send(messageToSend);
				return;
			case -1:
				LinkedHashMap s2=new LinkedHashMap();
				s2.put("event", "errorMessage");
				s2.put("value", "IOError");
				String message2Send=mapper.writeValueAsString(s2);
				System.out.println(message2Send);
				conn.send(message2Send);
				return;
			default:
				LinkedHashMap defaultClearMessage=new LinkedHashMap();
				defaultClearMessage.put("event", "clearOutputField");
				conn.send(mapper.writeValueAsString(defaultClearMessage));
				
				break;
			}

			ArrayList<LinkedHashMap> filterGroupString=(ArrayList<LinkedHashMap>)result.get("filterGroups");
			ArrayList<LinkedHashMap>filterGroups = filterGroupString;//;new ArrayList();
			
			int maxPages=Integer.parseInt((String) result.get("maxPages"));			//maximum pages to scan
			
			this.userDataList.put(username, new Userdata(username));				//the datastorage pojo, clear existing pojo
			this.scanOverviewPage(conn,linkString,maxPages,0,filterGroups,username,doDefault);		//start the scan
			
			Userdata thisUsersData=this.userDataList.get(username);					//after all the stuff is scanned, get the dataobj and return values to user
			System.out.println(thisUsersData.username+"-"+thisUsersData.getFlaggedCount());
			LinkedHashMap returnMap=thisUsersData.getDataMap(doDefault);
			
			String thisUsersDataString=mapper.writeValueAsString(returnMap);
			System.out.println(thisUsersDataString+"\n");
			conn.send(thisUsersDataString);
		}catch(ClassCastException ex){
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        catch(NullPointerException ex){
        	LinkedHashMap reply=new LinkedHashMap();
        	reply.put("event", "errorMessage");
			reply.put("value", "Scanning Error, Check your network filtering");
			String replyString;
			try {
				replyString = new ObjectMapper().writeValueAsString(reply);
				conn.send(replyString);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
        	ex.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    	System.out.println(ex.getMessage());
        System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
    }

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = Integer.parseInt(System.getenv("PORT"));
    
        WebSocketServer server = new redditWebSocketServer(new InetSocketAddress(host, port));
        server.run();
    }
    public int checkUserValidity(String linkString) throws SocketTimeoutException, HttpStatusException, IOException{
		Connection.Response response=null;
		try {
				 response= Jsoup.connect(linkString)
		            .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
		            .timeout(10000)
		            .execute();
				return response.statusCode();
		      }
		catch (HttpStatusException hre) {
	         System.out.println("io - "+hre.getStatusCode());
	         return hre.getStatusCode();
		}catch (IOException e) {
		         System.out.println("io - "+e);
		         return -1;
		   }
		
	}
	public int timeout=700; 		//timeout timer
	public void scanOverviewPage(WebSocket conn,String link,int maxPages,int pageNumber,
			ArrayList<LinkedHashMap>filterGroups,String username,boolean doDefault) throws MalformedURLException{		
		try{
			Document doc = Jsoup.parse(new URL(link), timeout);
			
			Userdata userfile=this.userDataList.get(username);			//copy of userfile
			

			int flaggedSubmissionCount=0,flaggedCommentsCount=0,flaggedCommentSubsCount=0, flaggedFilterGroupSubsCount=0;

			Element siteTable=doc.select("div#siteTable").first();		//ArrayList of elements
			Elements postSubreddits=siteTable.select("a[class=subreddit hover may-blank]");
			Elements comments=siteTable.select("div[class=md] > p");
			Elements commentsSubreddits=siteTable.select("a[class=subreddit hover]");

			
			for(Element postSubreddit:postSubreddits){						//for every entry in the arrayList
				String tagString=postSubreddit.text();				//get its name in lower case
				tagString.toLowerCase();							
				if(doDefault){
					for(String checkString:defaultSubFlags){				/*check:the defaults if true,each filter group*/
						if(tagString.equals(checkString)){
							flaggedSubmissionCount++;				//POSTING IN A DEFAULT FLAGGED SUB
							
						}
					}
				}
				int filterGroupsLength=filterGroups.size();
				for(int a=0;a<filterGroupsLength;a++){
					LinkedHashMap thisFilterGroup=filterGroups.get(a);
					ArrayList<String>subs= (ArrayList<String>) thisFilterGroup.get("subs");
					int subsLength=subs.size();
					for(int b=0;b<subsLength;b++){
						if(tagString.equals(subs.get(b))){
							flaggedFilterGroupSubsCount++;					//POSTING IN A FLAGGED FILTER GROUP
							userfile.AddSubFlagHit(tagString, 0);
							break;
						}
						
					}
				}
				/*if(doDefault){
					for(String slurString:defaultCommentFlags){
						slurString.toLowerCase();
						if(tagString.contains(slurString)||tagString.contains(slurString+'s')){
							flaggedCommentSubsCount++;
						}
					}
				}*/
				//System.out.println("tagString:"+tagString);
			}
			if(doDefault){
				for(Element comment:comments){
					String commentString=comment.text();
					for(String checkString:defaultCommentFlags){
						checkString.toLowerCase();
						commentString.toLowerCase();
						if(commentString.contains(checkString)||commentString.contains(checkString+'s')){
							flaggedCommentsCount++;
						}
					}
				}
			}

			for(Element commentSubreddits:commentsSubreddits){
				String tagString=commentSubreddits.text();
				tagString.toLowerCase();
				if(doDefault){
					for(String checkString:defaultSubFlags){
						if(tagString.equalsIgnoreCase(checkString)){
							flaggedCommentSubsCount++;
						}
					}
					
					for(String slurString:defaultCommentFlags){
						slurString.toLowerCase();
						if(tagString.contains(slurString)||tagString.contains(slurString+'s')){
							flaggedCommentSubsCount++;
						}
					}
				}
				int filterGroupsLength=filterGroups.size();
				for(int a=0;a<filterGroupsLength;a++){
					LinkedHashMap thisFilterGroup=filterGroups.get(a);
					ArrayList<String>subs=(ArrayList<String>) thisFilterGroup.get("subs");
					int subsLength=subs.size();
					for(int b=0;b<subsLength;b++){
						if(tagString.equals(subs.get(b))){
							flaggedFilterGroupSubsCount++;
							userfile.AddSubFlagHit(tagString, 0);
							break;
						}
					}
				}
			}
			userfile.flaggedCommentSubsCount+=flaggedCommentSubsCount;
			userfile.flaggedCommentsCount+=flaggedCommentsCount;
			userfile.flaggedSubmissionCount+=flaggedSubmissionCount;
			userfile.flaggedFilterGroupSubsCount+=flaggedFilterGroupSubsCount;
			this.userDataList.put(username, userfile);
			
			System.out.println("page#:"+pageNumber+"-"+link);
			LinkedHashMap s=new LinkedHashMap();
			s.put("event", "pageEvent");
			s.put("value", "page#:"+pageNumber+"-"+link);
			String messageToSend=new ObjectMapper().writeValueAsString(s);
			System.out.println(messageToSend);
			conn.send(messageToSend);
			if(maxPages==pageNumber){
				return;
			}
			this.scanOverviewPage(conn,siteTable.select("a[rel=nofollow next]").attr("href"), maxPages,pageNumber+=1,filterGroups, username,doDefault);
			
		}
		catch (SocketTimeoutException e) {
			this.scanOverviewPage(conn, link, maxPages, pageNumber,
					filterGroups,  username,doDefault);

		}
		catch (HttpStatusException e) {
			this.scanOverviewPage( conn,link, maxPages, pageNumber,
					filterGroups,  username,doDefault);

		}
		catch(MalformedURLException e){
			System.out.println("malformedUrl, pageNumber "+pageNumber);
			sendClientErrorMessage("MalformedURL, reddit down?",conn);
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("unknownIOEx");
			sendClientErrorMessage("NetworkError, reddit down?",conn);
			e.printStackTrace();
		}
		
	}
	public void sendClientErrorMessage(String msg,WebSocket conn){
		LinkedHashMap returnMap=new LinkedHashMap();
		returnMap.put("Event", "errorMessage");
		returnMap.put("value", msg);
		try {
			conn.send(new ObjectMapper().writeValueAsString(returnMap));
		} catch (NotYetConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private final String [] defaultSubFlags={"european","whiterights","KotakuInAction","TheRedPill",
			"Conspiracy","WhiteRights","sjwhate","drugs","TradicalChristianity","Kiketown","greatapes","nationalSocialism","blackCrime",
			"fatpeoplehate2","truewomensliberation","Askredpill" ,"redpillwomen","marriedredpill","n1ggers",
			"Teenapers","ChimpMusic","TrayvonMartin","ApeWrangling","TheRacistRedPill","USBlackCulture",
		"Apefrica","Detoilet","JustBlackGirlThings","BlackFathers","NegroFree","BlackHusbands","BlackCrime","gibsmedat","muhdick","didntdonuffins",
	"niglets","chimpout","Chicongo","TNB","ChimpireMETA","ChimpireOfftopic","Holocaust","AdolfHitler","AmericanJewishPower",
	"AntiPOZi","Awwschwitz","Blackplague","BritishJewishPower","Chicongo","Chimpire","Disciplined","Didntdonuffins",
	"DarkEnlightenment","Eugenics","Farright","Fascist","Ferguson","GoEbola","GoldenDawn","GreatAbos","GreatApes","HBD"
	,"JewishSupremacism","JustBlackGirlThings","KKK","Killwhitey","Muhdick","NationalSocialism","Nationalism","NegroFree",
	"New_right","Niglets","NorthwestFront","Polistan","Race_Realism","Race_reality",
	"Racism_immigration","SHHHHHEEEEEEEEIIIITT","Sheboonz","Starvin_marvins","TNB","TheProjects",
	"TheRacistRedPill","Third_position","TrayvonMartin","USBlackCulture","WhiteIdentity","WhiteNationalism","WhiteRights",
	"WhiteRightsUK","White_Pride","Whitebeauty","WhitesWinFights","WorldStarHP","ZOG","ZionistScum","CrackerTown",
	"blackcrimematters", "againstmensrights", "hotpeoplehate","subredditcancer","the_donald",
	
	"shitwhitepeoplesay","EbolaHoax"," WhitesWinFights"," Booboons"," angryblackladies"," bestofcoontown"," hatepire",
	"whitesmite","richpeoplehate","blackpeoplehate","fatwsgyhate","muslimpeoplehate","NationalNaziParty","whitesarecriminals",
	"FreddieGray","jewhate","libtards","blackhistorymonth","Fuck_The_Quran","religiouspeoplehate","FatHateLounge","LedariusWilliams",
	"RapeWorthy_Feminists","WhiteGentlemen","AgainstHarrietsRights","Jason_Harrison","funRAPINGWOMENstories","WhitePeopleRiots",
	"FatPeopleHateLounge","Drapetomania_Dynasty","IsaacHolmes","Ismaaiyl_Brinsley","Jordan_Mitchell","Mestizos","raping_spez","BeatingMuslims"
	};
	private final String[] defaultCommentFlags={"fag","kike","nigger","faggot","cuck"};

}
