import java.util.LinkedHashMap;


	public class Userdata{		
		
		String username;					//name of reddit user
		private int flagLevel=0;					//overall user rating
		int flaggedSubmissionCount=0,			//THIS IS COUNTER ON DEFAULTFILTER, POSTING IN A FLAGGED SUB
			flaggedCommentsCount=0,					//this is baisically the number oftimes the used the word cuck
			flaggedCommentSubsCount=0;					//removed 4 now, usage of cuck within titles
		
		int flaggedFilterGroupSubsCount=0;			//

		LinkedHashMap <String,MutableInt>subredditHits;


		public Userdata(String username){
			this.username=username;
		}
		public LinkedHashMap getDataMap(boolean doDefault){
			LinkedHashMap returnMap = new LinkedHashMap();
			returnMap.put("Event", "returnReport");
			returnMap.put("username", username);
			returnMap.put("doDefault", doDefault);
			returnMap.put("flaggedFilterGroupSubsCount",flaggedFilterGroupSubsCount);
			returnMap.put("subredditHits", subredditHits);
			if(doDefault){
				returnMap.put("flaggedSubmissionCount", flaggedSubmissionCount);
				returnMap.put("flaggedCommentsCount", flaggedCommentsCount);
				returnMap.put("flagLevel", this.getFlaggedCount());
				//returnMap.put("flaggedCommentSubsCount", flaggedCommentSubsCount);
			}
			else{
				
			}
			return returnMap;
		}
		public void AddSubFlagHit(String subName,int type){
			subredditHits.get(subName).increment();
		}
		public int getFlaggedCount(){
			this.flagLevel=flaggedSubmissionCount+flaggedCommentsCount+flaggedCommentSubsCount;
			return this.flagLevel;
		}
	}