package src;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashSet;
import java.util.TreeMap;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class redditWebcrawler {
	/*
	 * Search through a users comment and submission history and identify whether user is a disreputable.
	 * Usage of racist/mysoginistic/ellenpaoSJWcirclejerkConspiracy words/subs is used as an identifier
	 * 
	 * Webcrawler is a configuration, creates/saves Userdata classes, returns Userdata
	*/
	private TreeMap <String,Userdata> userDataList=new TreeMap<String,Userdata>();
	
	//default flagged subs and comments,
	//hate subs, racial slurs, hate echochambers
	//used to identify disreputable users,
	//note /r/european is a xenophobic hate sub
	private final String[] defaultCommentFlags={"fag","nigger","chink","faggot"};
	
	
	//The worst of the worst, users of these subs are flagged, any sub that contains one of the slurs is also included
	private final String [] defaultSubFlags={"CoonTown","european","whiterights","KotakuInAction","TheRedPill",
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
	"blackcrimematters", "againstmensrights", "hotpeoplehate","subredditcancer",
	
	"shitwhitepeoplesay","EbolaHoax"," WhitesWinFights"," Booboons"," angryblackladies"," bestofcoontown"," hatepire",
	"whitesmite","richpeoplehate","blackpeoplehate","fatwsgyhate","muslimpeoplehate","NationalNaziParty","whitesarecriminals",
	"FreddieGray","jewhate","libtards","blackhistorymonth","Fuck_The_Quran","religiouspeoplehate","FatHateLounge","LedariusWilliams",
	"RapeWorthy_Feminists","WhiteGentlemen","AgainstHarrietsRights","Jason_Harrison","funRAPINGWOMENstories","WhitePeopleRiots",
	"FatPeopleHateLounge","Drapetomania_Dynasty","IsaacHolmes","Ismaaiyl_Brinsley","Jordan_Mitchell","Mestizos","raping_spez","BeatingMuslims"

	};
	private final String[]defaultFlaggedReligiousSubs={
			//users of religious subs are not necessarily hateful but higher usage 
			//is an indicator of religious(or anti-religious) extremeism.
			"atheism","exmormon","exmuslim"
	};
	HashSet<String> flaggedCommentTermsList=new HashSet<String>();	//commonly used comment phrases
	HashSet <String> flaggedSubsList=new HashSet<String>();			//commonly used subreddits
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
	
	public void startCrawl(String username) throws MalformedURLException, IOException{
		String linkString="http://www.reddit.com/user/"+username;
		
			switch(checkUserValidity(linkString)){
			case 404:
				System.out.println("Not a valid username");
				return;
			case -1:
				System.out.println("IO error try again");
				return;
			default:
				break;
			}
			
		
		
		userDataList.put(username, new Userdata(username));
		this.scanOverviewPage(linkString,username,1);
		
		Userdata data=userDataList.get(username);
		System.out.println("user flag level:"+data.getFlaggedCount());
		String flagLevelResponse="";
		int flagLevel=data.getFlaggedCount();
		if(flagLevel==0){
			flagLevelResponse=flagLevel+":"+username+" is spotless";
		}
		else if(flagLevel>0&&flagLevel<5){
			flagLevelResponse=flagLevel+":"+username+" is mostly spotless";
		}
		else if(flagLevel>=5&&flagLevel<20){
				flagLevelResponse=flagLevel+":"+username+" is disreputable";
			}
		else if(flagLevel>=20&&flagLevel<50){
				flagLevelResponse=flagLevel+":"+username+" is very disreputable";
			}
		else if(flagLevel>=50){
				flagLevelResponse=flagLevel+":"+username+" is extremely disreputable with a high frequency of incidents";
			}
		System.out.println(flagLevelResponse);
	}
	
	public int timeout=700; 		//timeout timer
	public int stopPoint=5;			//maximum number of pages to search, -1 for infinite
	public void scanOverviewPage(String link,String username,int pageNumber) throws MalformedURLException{		
		//recursive, scan page, if true, return true, if not then return false and continue
		try {
			Document doc = Jsoup.parse(new URL(link), timeout);
			
			int flaggedSubmissionCount=0,flaggedCommentsCount=0,flaggedCommentSubsCount=0;

			Element siteTable=doc.select("div#siteTable").first();		//ArrayList of elements
			//Elements post=siteTable.select("p[class=tagline]");
			Elements postSubreddits=siteTable.select("a[class=subreddit hover may-blank]");
			Elements comments=siteTable.select("div[class=md] > p");
			Elements commentsSubreddits=siteTable.select("a[class=subreddit hover]");

			
			for(Element postSubreddit:postSubreddits){						//for every entry in the arrayList
				String tagString=postSubreddit.text();
				tagString.toLowerCase();
				for(String checkString:flaggedSubsList){
					if(tagString.equalsIgnoreCase(checkString)){
						flaggedSubmissionCount++;
						}
				}
				for(String slurString:flaggedCommentTermsList){
					slurString.toLowerCase();
					if(tagString.contains(slurString)||tagString.contains(slurString+'s')){
						flaggedCommentSubsCount++;
					}
				}
				//System.out.println("tagString:"+tagString);
			}
			for(Element comment:comments){
				String commentString=comment.text();
				for(String checkString:flaggedCommentTermsList){
					checkString.toLowerCase();
					commentString.toLowerCase();
					if(commentString.contains(checkString)||commentString.contains(checkString+'s')){
						flaggedCommentsCount++;
					}
				}
				//System.out.println("commentText:"+commentString);
			}

			for(Element commentSubreddits:commentsSubreddits){
				String tagString=commentSubreddits.text();
				tagString.toLowerCase();
				for(String checkString:flaggedSubsList){
					if(tagString.equalsIgnoreCase(checkString)){
						flaggedCommentSubsCount++;
					}
				}
				for(String slurString:flaggedCommentTermsList){
					slurString.toLowerCase();
					if(tagString.contains(slurString)||tagString.contains(slurString+'s')){
						flaggedCommentSubsCount++;
					}
				}
				//System.out.println("commentSub:"+commentString);
			}
			int totalFlagged=flaggedCommentSubsCount+flaggedCommentsCount+flaggedSubmissionCount;
			Userdata userfile=this.userDataList.get(username);
			userfile.flaggedCommentSubsCount+=flaggedCommentSubsCount;
			userfile.flaggedCommentsCount+=flaggedCommentsCount;
			userfile.flaggedSubmissionCount+=flaggedSubmissionCount;
			this.userDataList.put(username, userfile);
			
			//System.out.println(siteTable.select("a[rel=nofollow next]").attr("href"));
			System.out.println("page#:"+pageNumber+"-"+link);
			if(this.stopPoint==pageNumber){
				return;
			}
			this.scanOverviewPage(siteTable.select("a[rel=nofollow next]").attr("href"), username,pageNumber+=1);
			
		}
		catch (SocketTimeoutException e) {
			//System.out.println("SocketTimeout, pageNumber"+pageNumber);
			this.scanOverviewPage(link,username,pageNumber);

			//e.printStackTrace();
		}
		catch (HttpStatusException e) {
			//System.out.println("httpStatus, pageNumber"+pageNumber);
			this.scanOverviewPage(link,username,pageNumber);

//			e.printStackTrace();
		}
		catch(MalformedURLException e){
			System.out.println("malformedUrl, pageNumber "+pageNumber);
			//this.scanOverviewPage(link,username,pageNumber);			
			//e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public String getReport(String username){
		String userReport="";
		return userReport;
	}
	public redditWebcrawler(){
		
		int l=defaultCommentFlags.length;
		for(int a=0;a<l;a++){
			flaggedCommentTermsList.add(defaultCommentFlags[a]);
		}
		int l2=defaultSubFlags.length;
		for(int a=0;a<l2;a++){
			flaggedSubsList.add(defaultSubFlags[a]);
		}
	}
	
	public void AddFlaggedPhrase(String s){
		flaggedCommentTermsList.add(s);
	}
	public void AddFlaggedSub(String s){
		flaggedSubsList.add(s);
	}
	
}
