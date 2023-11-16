/**
 * COMP3100
 * S1 2022
 * Sandra Trinh
 * 45915881
 * sandra.trinh@students.mq.edu.au
 * March-April 2022
 * Stage 1 and Stage 2 code
 */
import java.io.*;
import java.net.*;

import javax.naming.CannotProceedException;

public class MyClient {

	private static Socket socket;
	private static BufferedReader in;
	private static DataOutputStream dout;
	private static String str="";
	private static int port = 50000;
	private static String ip= "localhost";

	private static boolean cannotFindServer = false;
	private static int maxCore = 0;
	private static String serverType="";
	private static int serverID=0;
	private static int serverIDMax=0;
	private static String[][] allServers;
	private static String[][] capableServers;
	private static String[][] lstjList;

	public static void main(String[] args)throws Exception{

		//Create a socket
		socket=new Socket(ip, port);
		//Initialise input and output streams associated with the socket
		//Connect ds-server
		in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
		dout=new DataOutputStream(socket.getOutputStream());
		
		//send HELO
		sendHELO();

		//get system user name
		String username = System.getProperty("user.name");
		//Send AUTH username
		sendAUTH(username);
		
		//while ds-server's msg is not NONE manage the JOBN			
		while(!isNONE(str)){
			//Send REDY
			sendREDY();

			//split the job information and put it in a String array 
			String[] jobInfo = str.split(" ");
			//check if the msg is a JOBN or JOBP
			//only SCHD if it is a JOBN or JOBP
			if(isJOBN(jobInfo[0])){

				if(args.length>0){// if there are arguments eg; '-a lrr'
					
					if(args[0].equals("-a")){//check correct command [-a]
						String algorithm = (args.length>1) ? args[1]:"lrr"; 
					switch (algorithm) {
						case "lrr":
							startLLR(jobInfo);//stage1
						break;
						case "superClient":
						startSuper(jobInfo);//stage2
						break;
						default:
							startLLR(jobInfo);//defualt to stage1 code
							System.out.println("Error "+algorithm+" is not reconised algorthm. Default to llr.");
					}
					} else { //error checking of arguments 
						System.out.println("Error "+ args[0]+" is not a reconised comand. Use [-a] for llr OR superClient. Example 'java MyClient -a llr' to run a large round robin Client");
						sendQUIT();

						//close the data stream and socket
						dout.close();
						socket.close();
						System.exit(0);
					}
				} else {
					startLLR(jobInfo); //defualt to stage 1 code
					//System.out.println("default to Large Round Robin");
				}
			};
		};

		sendQUIT();

		//close the data stream and socket
		dout.close();
		socket.close();
	};

	/**
	* send HELO to the server
	* @throws Exception
	*/
	private static void sendHELO() throws Exception{
		//send HELO
		dout.write(("HELO\n").getBytes());
		dout.flush();
		str=in.readLine();//server send OK
		//System.out.println(str);

	}

	/**
	* send AUTH to the server
	* @throws Exception
	*/
	private static void sendAUTH(String username) throws Exception{
		//Send AUTH username
		dout.write(("AUTH "+username+"\n").getBytes());
		dout.flush();
		str=in.readLine();//senver sends OK

	}

	/**
	* send REDY to the server
	* @throws Exception
	*/
	private static void sendREDY() throws Exception{
		//send REDY
		dout.write(("REDY\n").getBytes());
		dout.flush();
		str=in.readLine();// server send event
		//System.out.println(str);

	}

	/**
	* send OK to the server
	* @throws Exception
	*/
	private static void sendOK() throws Exception{
		dout.write(("OK\n").getBytes());
		dout.flush();
	}

	/**
	* send PSHJ to the server
	* push job, skips the current job let it come back again later as a JOBP
	* @throws Exception
	*/
	private static void sendPSHJ() throws Exception{
		dout.write(("PSHJ\n").getBytes());
		dout.flush();
		str=in.readLine();//server sends OK
		//System.out.println(str);
	}

	/**
	* send SCHD to the server
	* @param jobID String the job id to be sent
	* @param serverType String the server type to be used
	* @param serverID int the server id to be used
	* @throws Exception
	*/
	private static void sendSCHD(String jobID, String serverType, int serverID) throws Exception{
		//SCHD the JOBN the server sends over
		dout.write(("SCHD " +jobID+" "+serverType+" "+serverID+"\n").getBytes());
		dout.flush();
		str=in.readLine();//server sends OK
		//System.out.println(str);
	}

	/**
	 * send MIGJ to server, migrate a job
	 * @param jobID job id of the job to migrate
	 * @param srcServerType sorce server type of where the job is currently waiting on
	 * @param srcServerID sorce server id of where the job is currently waiting on
	 * @param tgtServerType target server type where the job is to move to
	 * @param tgtServerID target server id where the job is to move to
	 * @throws Exception
	 */
	private static void sendMIGJ(int jobID, String srcServerType, int srcServerID, String tgtServerType, int tgtServerID) throws Exception{
		//SCHD the JOBN the server sends over
		dout.write(("MIGJ " +jobID+" "+srcServerType+" "+srcServerID+" "+tgtServerType+" "+tgtServerID+"\n").getBytes());
		dout.flush();
		str=in.readLine();//server sends OK
		//sendOK();
	}

	/**
	 * send EJWT to server to query the server estimated waiting time
	 * @param serverType server type 
	 * @param serverID server id
	 * @return int, estimated waiting time of the server
	 * @throws Exception
	 */
	private static int sendEJWT(String serverType, int serverID) throws Exception{
		//EJWT this query the server's estimation wait time
		dout.write(("EJWT " +serverType+" "+serverID+"\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA #job length of each message
		//System.out.println(" estimated time is "+str);
		int ewt = Integer.parseInt(str);//estimated waiting time
	
		return ewt;
	}


	/**
	 * send LSTJ to server to query the list of running and waiting jobs
	 * @param serverType
	 * @param serverID
	 * @throws Exception
	 */
	private static void sendLSTJ(String serverType, int serverID) throws Exception{
		//SCHD the JOBN the server sends over
		dout.write(("LSTJ " +serverType+" "+serverID+"\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA #job length of each message
		//System.out.println(str);
		String[] dataInfo = str.split(" ");
		int numJobs = Integer.parseInt(dataInfo[1]);
		lstjList = new String[numJobs][];
		sendOK();//send ok
		for(int i=0; i<lstjList.length;i++){
			str=in.readLine();//current jog
			lstjList[i] = str.split(" ");
		}
		sendOK();
		str=in.readLine();//get dot
		
	//	System.out.println(str);
	}

	/**
	* send QUIT to the server
	* @throws Exception
	*/
	private static void sendQUIT() throws Exception{
		//send quit to server
		dout.write(("QUIT\n").getBytes());
		dout.flush();
		str=in.readLine();//receive QUIT from the server
		//System.out.println(str);
	}

	/**
	* check if the message returned from server is "NONE"
	* @return true if message is NONE else false otherwised
	*/
	private static boolean isNONE(String msg){
		return msg.equals("NONE");
	}

	/**
	* Check if the job is type JOBN
	* @param jobType String The job type. The first element the event sent from server.
	* @return true if it is a JOBN else false if not a JOBN.
	*/
	private static boolean isJOBN(String jobType)throws Exception{
		return jobType.equals("JOBN") || jobType.equals("JOBP");
	}

	/**
	 * send GETS All and update serverList.
	 */
	private static void sendGetAll()throws Exception{

		//get all the servers
		//jobINFO JOBN submitTime ID estRuntime core memory disk
		//GET Capable core memory disk 
		dout.write(("GETS All\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA nRecs recSize
		//System.out.println(str);

		//server send data server avalability 
		//split the DATA nRecs recSize into a string array
		String[] dataInfo = str.split(" ");
		//save the total number of servers into numServers
		int numServers = Integer.parseInt(dataInfo[1]);

		sendOK();
		
		//server send list of servers	
		allServers = new String[numServers][];
		//loop through servers
		for(int i = 0; i < numServers; i++){
			str=in.readLine();//current server
			//System.out.println(str);
			//split the str into an array and save into currentServer array
			String[] currentServer = str.split(" ");
			//save server in to list of servers.
			allServers[i]= currentServer;
		};
		sendOK();
		str=in.readLine();//get dot
	}

	/**
	 * send get capable to the server.
	 * Also save the list of server datas into the capableServer String [][].
	 * the following params are for the get capable dout write
	 * @param core
	 * @param memory
	 * @param disk
	 * @throws Exception
	 */
	private static void sendGetCapable(int core, int memory, int disk) throws Exception{
		//get capable the servers
		//jobINFO JOBN submitTime ID estRuntime core memory disk
		//GET Capable core memory disk 
		dout.write(("GETS Capable" +" "+core+" "+memory+" "+disk+ "\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA nRecs recSize
		//System.out.println(str);
		//if(str != null) {

		
		String[] dataInfo = str.split(" ");
		//save the total number of servers into numServers
		int numServers = Integer.parseInt(dataInfo[1]);
		sendOK();
		//System.out.println(str);
		//server send list of servers	

		//holds the list of servers data
		capableServers = new String[numServers][];
		//loop through all the servers
		//create an array of servers.
		
		for(int i = 0; i < numServers; i++){
			str=in.readLine();//current server
			//System.out.println(str);
			//split the str into an array and save into currentServer array
			String[] currentServer = str.split(" ");
			//save server in to list of servers.
			capableServers[i]= currentServer;
		};
		sendOK();
		str=in.readLine();//get dot

	}

	/**
	* This gets the largest server.
	* if there is more then one server type with the same largest core use the first server type
	* serverType - gets the largest server type
	* serverID - starts at 0
	* serverIDMax - gets the largest server's biggest ID number
	* @throws Exception
	*/
	private static void sendGetAllLarge()throws Exception{

		//get all the servers
		dout.write(("GETS All\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA nRecs recSize
		System.out.println(str);

		//server send data server avalability 
		//split the DATA nRecs recSize into a string array
		String[] dataInfo = str.split(" ");
		//save the total number of servers into numServers
		int numServers = Integer.parseInt(dataInfo[1]);

		sendOK();
		//System.out.println(str);
		//server send list of servers	
			
		//loop through all the servers
		for(int i = 0; i < numServers; i++){
			str=in.readLine();//current server
			
			//split the str into an array and save into currentServer array
			String[] currentServer = str.split(" ");

			//save the first server with the biggest core. 
			if(maxCore < Integer.parseInt(currentServer[4])){
				maxCore = Integer.parseInt(currentServer[4]);
				serverType=currentServer[0];
			};

			//if another server of the same type appear update the number of servers of the same type
			//update the serverIDMax
			if(serverType.equals(currentServer[0])){
				serverIDMax=Integer.parseInt(currentServer[1]);
			};	
		};

	}

	/**
	* Update the server ID. 
	* I the server ID is bigger then the serverIDMax reset the ID to zero '0'.
	* 
	* The allow the client to SCHD JOBNs in a round-robin fashion.
	* stage 1 assigment
	*/
	private static void updateServerID(){
		//increment the server id
		serverID++;
			
		//if server id is bigger then the max id reset to 0
		//LRR 
		if(serverID > serverIDMax){
			serverID=0;
		};
	}


	/**
	 * Send Get first capable server 
	 * GET Capable core memory disk 
	 * params jobInfo is a string Array with the below content/data
	 * @param jobInfo JOBN submitTime ID estRuntime core memory disk
	 * @throws Exception
	 */
	private static void sendGetFc(String[] jobInfo)throws Exception{

		//get all the servers
		//jobINFO JOBN submitTime ID estRuntime core memory disk
		//GET Capable core memory disk 
		dout.write(("GETS Capable" +" "+jobInfo[4]+" "+jobInfo[5]+" "+jobInfo[6]+ "\n").getBytes());
		dout.flush();
		str=in.readLine();//DATA nRecs recSize
		//System.out.println(str);

		//server send data server avalability 
		//split the DATA nRecs recSize into a string array
		String[] dataInfo = str.split(" ");
		//save the total number of servers into numServers
		int numServers = Integer.parseInt(dataInfo[1]);

		sendOK();
		System.out.println(str);
		//server send list of servers	
			
		//loop through all the servers
		for(int i = 0; i < numServers; i++){
			str=in.readLine();//current server
			System.out.println(str);
			//split the str into an array and save into currentServer array
			String[] currentServer = str.split(" ");

			//save the first server only
			if(i==0){
				//maxCore = Integer.parseInt(currentServer[4]);
				serverType=currentServer[0];
				serverID=Integer.parseInt(currentServer[1]);
			};//skip all othr servers
				
		};
	}

	private static String tinyServerType = "";
	private static String smallServerType = "";
	private static String mediumServerType = "";
	private static String largeServerType = "";
	private static String xlargeServerType = "";

	private static String tinyCore = "2";
	private static String smallCore = "4";
	private static String mediumCore = "8";
	private static String largeCore = "16";
	private static String xlargeCore = "32";

	/**
	 * set the server names, separated by core size.
	 * @throws Exception
	 */
	private static void setServerTypeNamesList()throws Exception{
		sendGetAll();//updates allServer String[][] list

		//check if they have the server type and get the server name.
		tinyServerType = getServerTypeByCore(allServers, tinyCore);
		smallServerType = getServerTypeByCore(allServers, smallCore);
		mediumServerType = getServerTypeByCore(allServers, mediumCore);
		largeServerType = getServerTypeByCore(allServers, largeCore);
		xlargeServerType = getServerTypeByCore(allServers, xlargeCore);
	}

	private static String getServerTypeByCore(String[][] serverList, String core){
		for(int i=0; i<serverList.length; i++){
			if(serverList[i][4].equals(core)){
				return serverList[i][0];
			}
		}
		return "";
	}

	/**
	 * get server for the super client stage 2 code.
	 * @param jobInfo string array of job infomation : JOBN submitTime ID estRuntime core memory disk
	 * @throws Exception
	 */
	private static void sendGetServer(String[] jobInfo)throws Exception{
		// //get capable the servers
		// //jobINFO JOBN submitTime ID estRuntime core memory disk
		// //GET Capable core memory disk 
		sendGetCapable(Integer.parseInt(jobInfo[4]), Integer.parseInt(jobInfo[5]), Integer.parseInt(jobInfo[6]));
	
		//defines small jobs are. jobs with small core requirements 
		int smallCoreLimit = 5;
		//check if the job is small
		//handle small jobs
		if(Integer.parseInt(jobInfo[4])<smallCoreLimit){
			int jobCore = Integer.parseInt(jobInfo[4]);//get job core
			String currentServerType="";
			int currentServerID=0;
			boolean findServer=true;
			int waitLimit = 3;
			int sIDLimitSmallJ = 2;
			//check if the best server is found for the job yet
			if(findServer){

				//check if there are any idle servers the job can be sent to
				for(int i=0; i<capableServers.length && findServer; i++){
					String serverState = capableServers[i][2];
					if(serverState.equals("idle")){
							currentServerType = capableServers[i][0];
							currentServerID= Integer.parseInt(capableServers[i][1]);
							findServer = false;
					}
				}

				//check if there are any small servers the job can be sent to
				if(findServer){
				for(int i=0; i<capableServers.length && findServer; i++){
					int sId = Integer.parseInt(capableServers[i][1]);
					int waitJob = Integer.parseInt(capableServers[i][7]);
					int serverCore = Integer.parseInt(capableServers[i][4]);
					if(capableServers[i][0].equals(smallServerType) && waitJob<1 && serverCore>=jobCore){	
						currentServerType = capableServers[i][0];
						currentServerID= Integer.parseInt(capableServers[i][1]);
						findServer = false;
					}
					
				}
				}

				//check if there are any medium servers the job can be sent to
				if(findServer){

					for(int i=0; i<capableServers.length && findServer; i++){
						int waitJob = Integer.parseInt(capableServers[i][7]);
						int serverCore = Integer.parseInt(capableServers[i][4]);
						int sID = Integer.parseInt(capableServers[i][1]);
						if(capableServers[i][0].equals(mediumServerType) && sID<(sIDLimitSmallJ) && waitJob<(waitLimit) && serverCore>=jobCore){	
							currentServerType = capableServers[i][0];
							currentServerID= Integer.parseInt(capableServers[i][1]);
							findServer = false;
						}
						
					}
				}

					//check if there are any large servers the job can be sent to
				if(findServer){

					for(int i=0; i<capableServers.length && findServer; i++){
						int waitJob = Integer.parseInt(capableServers[i][7]);
						int serverCore = Integer.parseInt(capableServers[i][4]);
						int sID = Integer.parseInt(capableServers[i][1]);
						if(capableServers[i][0].equals(largeServerType) && sID<sIDLimitSmallJ && waitJob<(waitLimit) && serverCore>=jobCore){	
							currentServerType = capableServers[i][0];
							currentServerID= Integer.parseInt(capableServers[i][1]);
							findServer = false;
						}
						
					}
				}

					//try servers that are not small or tiny
					if(findServer){
						int waitJobLimit = 0;
						while(findServer && waitJobLimit<4){
							waitJobLimit ++;
						for(int i=0; i<capableServers.length && findServer; i++){
							int waitJob = Integer.parseInt(capableServers[i][7]);
							if(!capableServers[i][0].equals(tinyServerType) || !capableServers[i][0].equals(smallServerType) ){
								if(waitJob<waitJobLimit){
									currentServerType = capableServers[i][0];
									currentServerID= Integer.parseInt(capableServers[i][1]);
									findServer = false;
								}
							}
						}}
						//try sending the job to the smallest estimation time with low number of waiting jobs
						if(findServer){
							String lServerType = capableServers[capableServers.length-1][0];
							int smallestWaitTime = sendEJWT(lServerType, Integer.parseInt(capableServers[capableServers.length-1][1]));;
							currentServerType = lServerType;
							currentServerID= Integer.parseInt((capableServers[capableServers.length-1][1]));
							for(int i=capableServers.length-1; i>-1; i--){
								int waitJob = Integer.parseInt(capableServers[i][7]);	
								int estimationWaitTime = sendEJWT(capableServers[i][0], Integer.parseInt(capableServers[i][1]));	
									if(estimationWaitTime<smallestWaitTime && estimationWaitTime>-1 && waitJob< waitJobLimit){
										smallestWaitTime = estimationWaitTime;
										currentServerType = capableServers[i][0];
										currentServerID= Integer.parseInt(capableServers[i][1]);
										findServer = false;
									}
							}	
						}
							//if no server is found yet, try all servers
							if(findServer){
								String lServerType = capableServers[0][0];
								int smallestWaitTime = sendEJWT(capableServers[capableServers.length-1][0], Integer.parseInt(capableServers[capableServers.length-1][1]));;
								currentServerType = lServerType;
								currentServerID= Integer.parseInt((capableServers[0][1]));
								
								//find server with small estimation wait time
								for(int i=0; i<capableServers.length && findServer; i++){
	
									int estimationWaitTime = sendEJWT(capableServers[i][0], Integer.parseInt(capableServers[i][1]));	
									if(estimationWaitTime<smallestWaitTime && estimationWaitTime>-1){
										smallestWaitTime = estimationWaitTime;
										currentServerType = capableServers[i][0];
										currentServerID = Integer.parseInt(capableServers[i][1]);
										findServer = false;
									}
								}

								//if JOBP still fail to find a server and comes back
								for(int i=0; i<capableServers.length && findServer; i++){
									int waitJob = Integer.parseInt(capableServers[i][7]);	
									if(waitJob<1 && jobInfo[0].equals("JOBP")){
										currentServerType = capableServers[i][0];
										currentServerID = Integer.parseInt(capableServers[i][1]);
										findServer = false;
									}
								}

								//push jobs if failed to find server for the job
								if(findServer){
									currentServerType = capableServers[capableServers.length-1][0];
									currentServerID = Integer.parseInt(capableServers[capableServers.length-1][1]);
									cannotFindServer = true;
									findServer = false;
									//System.out.println("no server 1");
								}
							}
					}

				}

			serverType=currentServerType;
			serverID=currentServerID;
			
		} else {// Handle bigger jobs
			String currentServerType="";
			int currentServerID=0;
			boolean findServer=true;
			int waitLimit = 0;
			int sIDLimit = 11;
			int sIDStart = 1;//start using the second server as the frist is reserved for small jobs
			//increase the wait limit to try to find server. waitLimit is capped
				while(waitLimit<2 && findServer){
					waitLimit ++;
					//try to find idle servers to send the job to
					for(int i=capableServers.length-1; i>-1 && findServer; i--){
						String serverState = capableServers[i][2];
						if(serverState.equals("idle")){
								currentServerType = capableServers[i][0];
								currentServerID= Integer.parseInt(capableServers[i][1]);
								findServer = false;
						}
					}
					
					System.out.println(xlargeServerType +" 1 "+ largeServerType+ " 2 "+ mediumServerType+" 3 "+smallServerType+ " 4 "+tinyServerType);
					//try the big server 
					if(findServer){
						for(int i=0; i<capableServers.length && findServer; i++){
							int waitJob = Integer.parseInt(capableServers[i][7]);
							int sID = Integer.parseInt(capableServers[i][1]);
							if(capableServers[i][0].equals(xlargeServerType)){
								if(waitJob<waitLimit){
									currentServerType = capableServers[i][0];
									currentServerID= Integer.parseInt(capableServers[i][1]);
									findServer = false;
								}
							}
						}
					}

					//try the large server
					if(findServer){
						for(int i=0; i<capableServers.length && findServer; i++){
					
							
							int waitJob = Integer.parseInt(capableServers[i][7]);
							int sID = Integer.parseInt(capableServers[i][1]);
							if(sID<sIDLimit && (capableServers[i][0].equals(largeServerType)) && sID>sIDStart){
								if(waitJob<waitLimit){
									currentServerType = capableServers[i][0];
									currentServerID= Integer.parseInt(capableServers[i][1]);
									findServer = false;
								}
							}
						}
					}

					//try the medium server
					if(findServer){
						for(int i=0; i<capableServers.length && findServer; i++){
							int waitJob = Integer.parseInt(capableServers[i][7]);
							int sID = Integer.parseInt(capableServers[i][1]);
							if(sID<sIDLimit && (capableServers[i][0].equals(mediumServerType)) && sID>sIDStart){
								if(waitJob<waitLimit){
									currentServerType = capableServers[i][0];
									currentServerID= Integer.parseInt(capableServers[i][1]);
									findServer = false;
								}
							}
						}
					}

						//look through all the server for 0 waiting job in the queue 
				if(findServer){
					for(int i=capableServers.length-1; i>-1 && findServer; i--){
						int waitJob = Integer.parseInt(capableServers[i][7]);	
						
							if(waitJob<waitLimit+5){	
								currentServerType = capableServers[i][0];
								currentServerID= Integer.parseInt(capableServers[i][1]);
								findServer = false;
							}
					}	
				}
				
				
				}

				//look through all the server for 0 waiting job in the queue 
				if(findServer){
					for(int i=capableServers.length-1; i>-1 && findServer; i--){
						int waitJob = Integer.parseInt(capableServers[i][7]);	
						
							if(waitJob<2){	
								currentServerType = capableServers[i][0];
								currentServerID= Integer.parseInt(capableServers[i][1]);
								findServer = false;
							}
					}	
				}
			//if failed to find server push the job
			if(findServer){
				cannotFindServer=true;
				findServer=false;
			}
			//save server information here to send to the schd
			serverType=currentServerType;
			serverID=currentServerID;
		}
			
	}

	/**
	 * Stage 2 methods
	 * check if waiting jobs can be migrated. 
	 * if so migrate the job
	 * @throws Exception
	 */
	private static void startMigration() throws Exception{
		sendGetAll(); //gets all the server information. updates allServers string []
		int longestEJWT = 0;
		String ejwtType = ""; //estimated job waiting time server type holder
		int ejwtID = 0;//estimated job waiting time holder
		boolean haveTime = false;//do we have a server with ejwt, if yes this is true
		for(int i=0; i < allServers.length && !haveTime; i++){
			String type = allServers[i][0];//current server type
			int id = Integer.parseInt(allServers[i][1]);// current server id
			int ejwt = sendEJWT(type, id);//call EJWT method to get the est
			if(ejwt>longestEJWT){//check if it is thebiggest time
				longestEJWT=ejwt;	//update biggest est
				ejwtType = type;// save server type and id
				ejwtID = id;
				haveTime = true;//set to true
			}
		}

		boolean haveWaitJob = false;//if there are any waiting jobs set to ture
		String[] waitingJob = new String[1];//waiting job information holder
		if(haveTime){//if we have server with long est time
			sendLSTJ(ejwtType, ejwtID);//updates lstjList[][] 
			
			//loop through the server's job list check and get one waiting job
			for(int i=0; i<lstjList.length && !haveWaitJob; i++){
				int jStartTime = Integer.parseInt(lstjList[i][3]);
				if(jStartTime == -1){
					waitingJob = lstjList[i];
					haveWaitJob = true;
				}
			}
		} 
		
		
		//call this method to get the waiting job capable servers and try to send job to an idle server and try to send to any server with no waiting job
		sendCapableAndMigrateJob(haveWaitJob, waitingJob, ejwtType, ejwtID);
	}

	/**
	 *  helper method for job migration. 
	 * Get capable servers and migrate to an idle server if there is one.
	 * 
	 * @param haveWaitingJob boolean, indicate if there is a job to find a server for
	 * @param waitingJob a String[] with job information for the job you want to migrate 
	 * @param serverType old server type name
	 * @param serverID old server id
	 * @throws Exception
	 */
	private static void sendCapableAndMigrateJob(boolean haveWaitingJob, String[] waitingJob, String serverType, int serverID) throws Exception{

		boolean finMigj=false;
		if(haveWaitingJob){
			sendGetCapable(Integer.parseInt(waitingJob[5]), Integer.parseInt(waitingJob[6]),Integer.parseInt(waitingJob[7]));
			//find idle server
			for(int i=0; i<capableServers.length && !finMigj;i++){
				String type = capableServers[i][0];
				int id = Integer.parseInt(capableServers[i][1]);
				String state = capableServers[i][2];
				if(state.equals("idle")){
					sendMIGJ(Integer.parseInt(waitingJob[0]), serverType, serverID, type, id);
					finMigj=true;
				}
			}

			//if still no server found
			//try to find any server with not waiting jobs
			for(int i=capableServers.length-1; i>-1 && !finMigj;i--){
				String type = capableServers[i][0];
				int id = Integer.parseInt(capableServers[i][1]);
				int waitJob = Integer.parseInt(capableServers[i][7]);
				if(waitJob<1){
					sendMIGJ(Integer.parseInt(waitingJob[0]), serverType, serverID, type, id);
					finMigj=true;
				}
			}

		}
	}

	/**
	 * large round robin 
	 * for stage 2
	 * COMP3100 MQ 2022 S1
	 * @param jobInfo string array of job infomation : JOBN submitTime ID estRuntime core memory disk
	 * @throws Exception
	 */
	 private static void startLLR(String[] jobInfo)throws Exception{
		 //Start LRR code

		 sendGetAllLarge(); // get the largest server
		 sendOK();
		 str=in.readLine(); //server send a dot '.'
		 //System.out.println(str);

		 //SCHD the JOBN the server sends over
		 sendSCHD(jobInfo[2], serverType, serverID);
		 
		 updateServerID();//mayneed to put in sendGerAll() method, as it only applays to LRR
		 //end LRR code
	 }

	 /**
	  * stage 2 code 
	  * @param jobInfo
	  * @throws Exception
	  */
	 private static void startSuper(String[] jobInfo) throws Exception{
		 if(jobInfo[2].equals("0")){
			setServerTypeNamesList(); 
		 }
		 //start stage 2 code
		 //get server information
		sendGetServer(jobInfo);
	//	sendOK();
	//	str=in.readLine(); //server send a dot '.'

		if(cannotFindServer){
			sendPSHJ();
			cannotFindServer=false;
		} else {
			//SCHD the JOBN the server sends over
			sendSCHD(jobInfo[2], serverType, serverID);
		}

		for(int i=0; i<6; i++){
			startMigration();
		}
	 }
};
