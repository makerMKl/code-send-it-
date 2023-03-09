package logic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import akka.actor.ActorRef;
import commands.BasicCommands;
import logic.TheOneAI;
import logic.TheOneConfig;
import logic.TheOneSquare;
import logic.TheOnelist;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;
import java.util.Random;
//-- create by MK.L 23.2.6
public class TheOneFunc_Logic {
	

	public static TheOneSquare[][] arry=new TheOneSquare[9][5];
	// -- Mark each coordinate and check if the status is unit or player
	public static TheOnelist theOnelist = new TheOnelist();			// -- 6 limit Hand List..
	public static TheOnelist theOnelistToAll = new TheOnelist();	// -- Deck Code
	public static TheOnelist theOneAilist = new TheOnelist();		// -- AI Deck
	public static String configdata = "";
	public static String cardName = "";
	
	public static int cID = 0;
	public static int gameStatus = 1; // -- 0 end 1 running
	
	public static Unit[] typeUnit  = new Unit[4096];
	public static Tile[][] publicTiles = new Tile[9][5];

	// luo Put the initialized player and AI inside the global to facilitate net function calls.
	public static Player humanPlayer = new Player(20, 2);
	public static Player aiPlayer = new Player(20, 2);

	/*
	 * name: modelInit_Begin
	 * using: For initialisation of the game interface etc and begin.
	 * input: ActorRef out
	 * coder: MK.L   2023.2.6 Xiuqi Liu 2773750
	 * change:
	 */	
	public static void modelInit_Begin(ActorRef out) throws IOException {
		
		BasicCommands.addPlayer1Notification(out, "Hello loser", 2);	// -- 欢迎语

		// -- Round initialization
		TheOneConfig.iEnTurnClick = 0;// -- End-of-round flag cleared
		
		TheOneConfig.roundStatus=1;	// -- Human rounds
		
		TheOneConfig.iUnitID =0;	// -- Unit ID initialisation
		gameStatus = 1;				// -- Game Over Status
		// -- Board initialisation
		publicTiles = func_CreateBoard(publicTiles,out);
		
		BasicCommands.addPlayer1Notification(out, "Game init", 2);	// -- init
		funcSleep(5);
		//sqare Initialisation
		for (int j = 0; j < 9; j++)
		{
			for (int i = 0; i < 5; i++)
			{
				//Storage square object to Two-dimensional arrays
				arry[j][i]=new TheOneSquare(j,i,false,false);
			}
		}

		humanPlayer = new Player(20, 2);
		aiPlayer = new Player(20, 2);
		mainShowplayerInit(out,humanPlayer,aiPlayer);
		funcSleep(100);
		
		// -- avatar init
		funcAvatarInit(out);
		// -- human Hand initialisation
		funcDeckInit(TheOneConfig.gCardNumAll[0],20);
		// -- Card Updates
		func_CardListUpdate(out);
		funcSleep(5);
		
		boolean gameStatus = true;
		while(gameStatus)
		{
			break;
		}
		
	}
	/*
	 * name: mainShowplayerInit
	 * using: Init player information
	 * input: ActorRef out
	 * coder: MK.L   2023.2.28 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int mainShowplayerInit(ActorRef out,Player hPlayer,Player aiPlayer) 
	{
		int iRet = 0;

		// -- blood
		BasicCommands.setPlayer1Health(out, hPlayer);
		// -- Mana
		BasicCommands.setPlayer1Mana(out, hPlayer);
		funcSleep(5);
		// -- blood
		BasicCommands.setPlayer2Health(out, aiPlayer);
		// -- Mana
		BasicCommands.setPlayer2Mana(out, aiPlayer);
		
		return iRet;
	}
	/*
	 * name: funcAvatarInit
	 * using: Init avatar para
	 * input: ActorRef out
	 * coder: MK.L   2023.2.28 Xiuqi Liu 2773750
	 * change:
	 */	
	public static void funcAvatarInit(ActorRef out) 
	{
		// -- Initialize the main monster
		Tile humanTile = BasicObjectBuilders.loadTile(1, 2);
		
		funcSquareStatusUpdate(out,1,2,98,1,true);
		typeUnit[98] = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar,
				TheOneConfig.iUnitID, Unit.class);
		// -- Extracting the deck number
		typeUnit[98].setiAttribution(1);
		TheOneConfig.iUnitID++;
		typeUnit[98].setPositionByTile(humanTile); 
		typeUnit[98].setBlood(20);
		
		BasicCommands.drawUnit(out, typeUnit[98], humanTile);
		funcSleep(100);
		addUnitAttack(out,98,2);
		addUnitHealth(out,98,20);
		Tile aiTile = BasicObjectBuilders.loadTile(7, 2);
		funcSleep(5);
		//Initialisation AIlist
		TheOneAI.AiInitList(out);
	
		funcSquareStatusUpdate(out,7,2,99,2,true);
		typeUnit[99] = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar,
				TheOneConfig.iUnitID, Unit.class);
		// -- Extracting the deck number
		typeUnit[99].setiAttribution(2);
		TheOneConfig.iUnitID++;
		typeUnit[99].setPositionByTile(aiTile); 
		BasicCommands.drawUnit(out, typeUnit[99], aiTile);
		funcSleep(100);
		typeUnit[99].setBlood(20);
		
		addUnitAttack(out,99,2);
		addUnitHealth(out,99,20);
		funcSleep(5);
	}

	/*
	 * name: addUnitAttack
	 * using: Increase UNIT attack 添加攻击
	 * input: ActorRef out,int ID,int iAttack
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int addUnitAttack(ActorRef out,int ID,int iAttack) {
		int iRet = 99;
		typeUnit[ID].setiAttack(iAttack);
		BasicCommands.setUnitAttack(out, typeUnit[ID], iAttack);
		return iRet;

	}
	
	/*
	 * name: addUnitHealth
	 * using: Increase unit blood volume 增加unit血量
	 * input: ActorRef out,int ID,int iHealth
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int addUnitHealth(ActorRef out,int ID,int iHealth) {
		int iRet = 99;
		typeUnit[ID].setHealth(iHealth);
		BasicCommands.setUnitHealth(out, typeUnit[ID], iHealth);
		if(ID == 98)
		{
			humanPlayer.setHealth(iHealth);
			funcSilverguard_Knight(out);
		}
		else if(ID == 99)
		{
			aiPlayer.setHealth(iHealth);
		}
		if(iHealth == 0)
		{
			iRet = 0;
		}
		return iRet;
	}

	/*
	 * name: compareConfigToiAttribution
	 * using: Read moralconfig from configuration file to get Attribution
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToiAttribution(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiAttribution();
		    }
		}
		return iRet;

	}
	
	/*
	 * name: compareConfigToiType
	 * using: Read moralconfig from configuration file to get iType
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToiType(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiType();
		    }
		}
		return iRet;

	}
	/*
	 * name: compareConfigToNO
	 * using: Read moralconfig from configuration file to get NO
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToNO(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
		    if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiNO();
		    }
		}
		return iRet;

	}
	/*
	 * name: compareConfigToHealth
	 * using: Read moralconfig from configuration file to get Health
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToHealth(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiHealth();
		    }
		}
		return iRet;

	}
	/*
	 * name: compareConfigToMana
	 * using: Read moralconfig from configuration file to get Mana
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToMana(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getManacost();
		    }
		}
		return iRet;

	}
	/*
	 * name: compareConfigToAttack
	 * using: Read moralconfig from configuration file to get Attack
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToAttack(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiAttack();
		    }
		}
		return iRet;

	}
	/*
	 * name: compareConfigToFunction
	 * using: Read moralconfig from configuration file to get Function
	 * input: String strConfig,int ID
	 * coder: MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int compareConfigToFunction(String strConfig,int ID) {
		int iRet = 99;
		for (int i = 0; i < 20; i++) {
			if (strConfig.equals(theOnelistToAll.get(i).getUnitConfig())&&ID == theOnelistToAll.get(i).getId())
		    {
		    	iRet = theOnelistToAll.get(i).getiFunction();
		    }
		}
		return iRet;

	}
	/*
	 * name: funcDeckInit
	 * using: Deck Initiation
	 * input: iCardNum int iDeck
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change: add AI card 3.1 2023
	 */	
	public static void funcDeckInit(int iCardNum, int iDeck) throws IOException {
	    LinkedList<Card> allCards = readIniConfiguration("app/Cardconfig.ini");
	    theOnelist.clear();
	    theOnelistToAll.clear();
	    for (int i = 0; i < iDeck; i++) {
            Card card = allCards.get(i);
            theOnelistToAll.add(BasicObjectBuilders.loadCard(
                    card.getCardConfig(), i, Card.class));
            theOnelistToAll.get(i).setCardConfig(card.getCardConfig());
            theOnelistToAll.get(i).setUnitConfig(card.getUnitConfig());
            theOnelistToAll.get(i).setiNO(card.getiNO());
            theOnelistToAll.get(i).setiType(card.getiType());
            theOnelistToAll.get(i).setiHealth(card.getiHealth());
            theOnelistToAll.get(i).setiAttack(card.getiAttack());
            theOnelistToAll.get(i).setManacost(card.getManacost());
            theOnelistToAll.get(i).setiAttribution(card.getiAttribution());
            theOnelistToAll.get(i).setManacost(card.getManacost());
            theOnelistToAll.get(i).setiFunction(card.getiFunction());
            theOnelistToAll.get(i).setCardname(card.getCardname());
            
	    }

	    for (int j = 0; j < iCardNum; j++) {
	        theOnelist.add(theOnelistToAll.get(j));
	    }

		//Initializing AI decks
		for (int y = 20; y < 40; y++) {
			Card aiCard = allCards.get(y);
			theOneAilist.add(BasicObjectBuilders.loadCard(
					aiCard.getCardConfig(), y, Card.class));
			theOneAilist.get(y-20).setCardConfig(aiCard.getCardConfig());
			theOneAilist.get(y-20).setUnitConfig(aiCard.getUnitConfig());
			theOneAilist.get(y-20).setiNO(aiCard.getiNO());
			theOneAilist.get(y-20).setiType(aiCard.getiType());
			theOneAilist.get(y-20).setiHealth(aiCard.getiHealth());
			theOneAilist.get(y-20).setiAttack(aiCard.getiAttack());
			theOneAilist.get(y-20).setManacost(aiCard.getManacost());
			theOneAilist.get(y-20).setiAttribution(aiCard.getiAttribution());
			theOneAilist.get(y-20).setManacost(aiCard.getManacost());
			theOneAilist.get(y-20).setiFunction(aiCard.getiFunction());
			theOneAilist.get(y-20).setCardname(aiCard.getCardname());
		}

	    TheOneConfig.gDeckCardNumAll[0] = 3;
	}
	
	/*
	 * name: readIniConfiguration
	 * using: Parsing the configuration set to form a card pack
	 * input: String filename
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */	
	public static LinkedList<Card> readIniConfiguration(String filename) throws IOException {
	    BufferedReader reader = null;
	    try {
	        reader = new BufferedReader(new FileReader(filename));
	        String line;
	        LinkedList<Card> cards = new LinkedList<>();
	        Card currentCard = null;

	        while ((line = reader.readLine()) != null) {
	            int semicolonIndex = line.indexOf(";");
	            if (semicolonIndex >= 0) {
	                line = line.substring(0, semicolonIndex);
	            }
	            line = line.trim();

	            if (line.startsWith("[")) {
	                if (currentCard != null) {
	                    cards.add(currentCard);
	                }
	                currentCard = new Card();
	            } else if (line.contains("=")) {
	                String[] parts = line.split("=");
	                String paramName = parts[0].trim();
	                String paramValue = parts[1].trim();

	                if (paramName.equals("CardConfig")) {
	                    currentCard.setCardConfig(paramValue);
	                } else if (paramName.equals("NO")) {
	                    currentCard.setiNO(Integer.parseInt(paramValue));
	                } else if (paramName.equals("Name")) {
	                    currentCard.setCardname(paramValue);
	                } else if (paramName.equals("UnitConfig")) {
	                    currentCard.setUnitConfig(paramValue);
	                } else if (paramName.equals("Type")) {
	                    currentCard.setiType(Integer.parseInt(paramValue));
	                }else if (paramName.equals("mana")) {
	                    currentCard.setManacost(Integer.parseInt(paramValue));
	                }else if (paramName.equals("attack")) {
	                    currentCard.setiAttack(Integer.parseInt(paramValue));
	                }else if (paramName.equals("health")) {
	                    currentCard.setiHealth(Integer.parseInt(paramValue));
	                }else if (paramName.equals("Attribution")) {
	                    currentCard.setiAttribution(Integer.parseInt(paramValue));
	                }else if (paramName.equals("function")) {
	                    currentCard.setiFunction(Integer.parseInt(paramValue));
	                }
	            } else {
	                if (currentCard != null) {
	                    cards.add(currentCard);
	                    currentCard = null;
	                }
	            }
	        }

	        if (currentCard != null) {
	            cards.add(currentCard);
	        }

	        return cards;
	    } finally {
	        if (reader != null) {
	            reader.close();
	        }
	    }
	}
	/*
	 * name: funcSleep
	 * using: Write a thread delay function as required
	 * input: int iMs
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */	
	public static void funcSleep(int iMs)
	{
		try {
				Thread.sleep(iMs);
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
	}


	/*
	 * name: getRandomArrangement
	 * using: This function first creates an array of 10 numbers, each of which appears twice, 
	and then uses the Random class to break up the elements of the array 
	into a random order. The function returns the resulting array.
	 * input: int iMs
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */	
	    public static int[] getRandomArrangement() {
	        int[] iID = new int[20];
	        for (int i = 0; i < 10; i++) 
	        {
	        	iID[i*2] = i;
	        	iID[i*2 + 1] = i;
	        }
	        
	        Random rand = new Random();
	        for (int i = 0; i < iID.length; i++) 
	        {
	            int randomIndex = rand.nextInt(iID.length);
	            int temp = iID[i];
	            iID[i] = iID[randomIndex];
	            iID[randomIndex] = temp;
	        }
	        
	        return iID;
	    }


		/*
		 * name: func_EnterTurn
		 * using: Round conversion flag function
		 * input: int iType iType 0 anen 1 en,int iId ,ActorRef out
		 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int func_EnterTurn(int iType,int iId ,ActorRef out) 
	    {
	    	int iRet = 0;
	    	if(iId == 0)// -- player
	    	{
		    	if(iType == 1)
		    	{
		    		func_CardListadd(1,0);
		    		func_CardListUpdate(out);
		    	}
	    	}

	        return iRet;
	    }
	    public static boolean isOneGridAway(int x, int y, int centerX, int centerY) {
	        int dx = Math.abs(x - centerX);
	        int dy = Math.abs(y - centerY);
	        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1) || (dx == 1 && dy == 1);
	    }
		/*
		 * name: func_CreateBoard
		 * using: Generating a chessboard
		 * input: Tile[][] aTiles,ActorRef out
		 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static Tile[][] func_CreateBoard(Tile[][] aTiles,ActorRef out) 
	    {
			Tile[][] tiles = aTiles;
			// -- board
			for(int i = 0;i<5;i++)
			{
				for(int j = 0;j<9;j++)
				{
					
					tiles[j][i]= BasicObjectBuilders.loadTile(j, i);
					BasicCommands.drawTile(out, tiles[j][i], 0);
					
				}
				funcSleep(1);
			}
			aTiles = tiles;
			return tiles;
	    }
		/*
		 * name: func_UnitMove
		 * using: Unit card movement logic
		 * input: yfirst true y false x ActorRef out, Unit unit, Tile tile
		 * coder: MK.L   2023.2.15 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int func_UnitMove(ActorRef out, Unit unit, Tile tile, boolean yfirst)
	    {
	    	int iRet = 0;
	    	
	    	BasicCommands.moveUnitToTile(out, unit, tile,yfirst);

	    	return iRet;
	    }
		/*
		 * name: func_UnitAnimation
		 * using: Unit card attack logic
		 * input: ActorRef out, Unit unit, UnitAnimationType animationToPlay
		 * 	idle,
			death,
			attack,
			move,
			channel,
			hit
		 * coder: MK.L   2023.2.15 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int func_UnitAnimation(ActorRef out, Unit unit, UnitAnimationType animationToPlay)
	    {
	    	int iRet = 0;
	    	if(animationToPlay == UnitAnimationType.idle)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	else if(animationToPlay == UnitAnimationType.death)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	else if(animationToPlay == UnitAnimationType.attack)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	else if(animationToPlay == UnitAnimationType.move)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	else if(animationToPlay == UnitAnimationType.channel)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	else if(animationToPlay == UnitAnimationType.hit)
	    	{
	    		BasicCommands.playUnitAnimation(out, unit, animationToPlay);
	    	}
	    	return iRet;
	    }
	    
		/*
		 * name: func_Move
		 * using: Unit movement logic
		 * input: ActorRef out, int iX, int iY,int iStatus
		 * coder: MK.L   2023.2.15 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static void func_Move(ActorRef out, int iX, int iY,int iStatus)
	    {
	    	int ID = arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID();
	    	boolean iRet = false;
	    	// -- Preventing the penetration of enemy models
	    	if(TheOneConfig.clickPosition[1]+1<=4 && TheOneConfig.clickPosition[0]-1>=0&&TheOneConfig.clickPosition[0]+1<=8&&TheOneConfig.clickPosition[1]-1>=0)
	    	{
	    		iRet = ((arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].isPlayer()||
		    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].isUnit())&&
		    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].getSquareStatus() 
		    			!= typeUnit[ID].getiAttribution()) || ((arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].isPlayer()||
		    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].isUnit())&&
		    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].getSquareStatus() 
		    	    			!= typeUnit[ID].getiAttribution());
	    	}
	    	else
	    	{
	    		if(TheOneConfig.clickPosition[1] == 4)// -- bottom
	    		{
	    			if((arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].isPlayer()||
	    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].isUnit())&&
	    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]-1].getSquareStatus() 
	    	    			!= typeUnit[ID].getiAttribution())
	    					{
	    						iRet = true;
	    					}
	    			else
	    			{
	    				iRet = false;
	    			}
	    		}
	    		else if(TheOneConfig.clickPosition[1] == 0)// top
	    		{
	    			if((arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].isPlayer()||
	    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].isUnit())&&
	    	    			arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]+1].getSquareStatus() 
	    	    			!= typeUnit[ID].getiAttribution())
	    					{
	    						iRet = true;
	    					}
	    			else
	    			{
	    				iRet = false;
	    			}
	    		}
	    		else if(TheOneConfig.clickPosition[0] == 8)// right
	    		{
	    			if((arry[TheOneConfig.clickPosition[0]-1][TheOneConfig.clickPosition[1]].isPlayer()||
	    	    			arry[TheOneConfig.clickPosition[0]-1][TheOneConfig.clickPosition[1]].isUnit())&&
	    	    			arry[TheOneConfig.clickPosition[0]-1][TheOneConfig.clickPosition[1]].getSquareStatus() 
	    	    			!= typeUnit[ID].getiAttribution())
	    					{
	    						iRet = false;
	    					}
	    			else
	    			{
	    				iRet = true;
	    			}
	    		}
	    		else if(TheOneConfig.clickPosition[0] == 0)// left
	    		{
	    			if((arry[TheOneConfig.clickPosition[0]+1][TheOneConfig.clickPosition[1]].isPlayer()||
	    	    			arry[TheOneConfig.clickPosition[0]+1][TheOneConfig.clickPosition[1]].isUnit())&&
	    	    			arry[TheOneConfig.clickPosition[0]+1][TheOneConfig.clickPosition[1]].getSquareStatus() 
	    	    			!= typeUnit[ID].getiAttribution())
	    					{
	    						iRet = false;
	    					}
	    			else
	    			{
	    				iRet = true;
	    			}
	    		}
	    	}
	    	if(iRet)
	    	{
	    		func_UnitMove(out,TheOneConfig.clickUnit,publicTiles[iX][iY],false);// -- 横走
	    	}
	    	else
	    	{
	    		func_UnitMove(out,TheOneConfig.clickUnit,publicTiles[iX][iY],true);// -- 纵走
	    	}
	    	TheOneFunc_Logic.funcSleep(5);
	    	// -- 如果没有移动过
			if(typeUnit[ID].getiMove() == 0)
	    	{
				typeUnit[ID].
						setPositionByTile(publicTiles[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]);
				typeUnit[ID].setiMove(1);
				//luo After the move. Clear the status of the previous position
				initPositionStatus(out,TheOneConfig.clickPosition[0],TheOneConfig.clickPosition[1]);
				displayMovePosition(out,iX,iY,99);
				TheOneFunc_Logic.funcSleep(5);
				funcSquareStatusUpdate(out,iX,iY,ID,iStatus,true);
				TheOneConfig.clickPositionStatus=0;
	    	}
			else	// -- 1 moved 3 attacked
			{
				// -- already move
			}

	    }

		/*
		 * name: func_CardListUpdate
		 * using: update card list Delete the original six listings that are larger than six positions
		 * input: ActorRef out
		 * coder: change MK.L   2023.2.23 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static void func_CardListUpdate(ActorRef out)
	    {
			for(int i = 1; i <= 6; i++) 
			{
					BasicCommands.deleteCard(out , i);
			}
			if(theOnelist.getSize()>5)
			{
				theOnelist.remove(6);
				theOnelist.remove(7);
				theOnelist.remove(8);
			}
			for(int i = 1; i <= theOnelist.getSize(); i++) 
			{
				if(theOnelist.get(i-1) != null)
				{
					BasicCommands.drawCard(out, theOnelist.get(i-1), i, 0);
				}
			}
	    }
	    
		/*
		 * name: func_ParaInit
		 * using: All status updates
		 * input: ActorRef out
		 * coder: change MK.L   2023.2.23 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static void func_ParaInit(ActorRef out)
	    {
			func_CardListUpdate(out);
			funcSleep(2);
			TheOneConfig.iCardNeedToPut = 0;
			TheOneConfig.clickPositionStatus=0;
			TheOneConfig.clickPosition[0] = 99;
			TheOneConfig.clickPosition[1] = 99;
			displayMovePosition(out,99,99,99);
			TheOneConfig.clickUnit = null;
	    }
	    
	    // -- Counterattack
		/*
		 * name: func_ParaInitCounterattack
		 * using: Counterattack
		 * input: ActorRef out,int iX,int iY
		 * coder: change MK.L   2023.2.23 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int func_ParaInitCounterattack(ActorRef out,int iX,int iY)
	    {	    
	    	int iRet = 0;
			displayMovePosition(out,99,99,99);
			// -- Player attacks
			func_UnitAnimation(out,typeUnit[arry[iX][iY].getiID()],
					UnitAnimationType.attack);
			
			funcSleep(1000);
			
			// -- Opponent drops blood
			int iLife = TheOneConfig.clickUnit.getHealth()
			-typeUnit[arry[iX][iY].getiID()].getiAttack();
			
			if(iLife<0)
			{
				iLife = 0;
			}
			iRet = addUnitHealth(out,arry[TheOneConfig.
			       clickPosition[0]][TheOneConfig.clickPosition[1]].getiID(),iLife);
			if(iRet == 0)// -- death
			{
				func_UnitAnimation(out,typeUnit[arry[TheOneConfig.
				       clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()],
						UnitAnimationType.death);
	
				funcSleep(1000);
				if(typeUnit[99].getHealth() == 0||typeUnit[98].getHealth() == 0)
				{
					TheOneFunc_Logic.humanPlayer.setHealth(TheOneFunc_Logic.typeUnit[98].getHealth());
					TheOneFunc_Logic.aiPlayer.setHealth(TheOneFunc_Logic.typeUnit[99].getHealth());
					BasicCommands.setPlayer1Health(out, TheOneFunc_Logic.humanPlayer);
					BasicCommands.setPlayer2Health(out, TheOneFunc_Logic.aiPlayer);
					BasicCommands.addPlayer1Notification(out, "Game Over", 200);
					TheOneFunc_Logic.gameStatus = 0;
					displayOver(out);
				}
				else
				{
					BasicCommands.addPlayer1Notification(out, "Unit lost", 2);
					BasicCommands.deleteUnit(out,TheOneConfig.clickUnit);
					initPositionStatus(out,TheOneConfig.clickPosition[0],TheOneConfig.clickPosition[1]);
				}
				iRet = -1;
			}
			else
			{
				if(arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID() ==99||
						arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()  ==98)
				{
					BasicCommands.setPlayer1Health(out, humanPlayer);
					BasicCommands.setPlayer2Health(out, aiPlayer);
				}

				func_UnitAnimation(out,typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()],
						UnitAnimationType.hit);
	            TheOneFunc_Logic.funcSleep(20);
	            TheOneFunc_Logic.func_UnitAnimation(out,typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()],
	                    UnitAnimationType.idle);
	            TheOneFunc_Logic.func_UnitAnimation(out,TheOneFunc_Logic.typeUnit[TheOneFunc_Logic.arry[iX][iY].getiID()],
	                    UnitAnimationType.idle);
			}
			return iRet;
	    }
	    
	    // -- Attack Logic
		/*
		 * name: func_ParaInit
		 * using: attack
		 * input: ActorRef out,int iX,int iY
		 * coder: change MK.L   2023.2.23 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int func_ParaInit(ActorRef out,int iX,int iY)
	    {	    
	    	int iRet = 0;
			displayMovePosition(out,99,99,99);
			// -- Player attacks
			func_UnitAnimation(out,TheOneConfig.clickUnit,
					UnitAnimationType.attack);
			funcSleep(1000);
			
			// -- Opponent drops blood
			int iLife = typeUnit[arry[iX][iY].getiID()].getHealth()
			-TheOneConfig.clickUnit.getiAttack();
			if(iLife<0)
			{
				iLife = 0;
			}
			iRet = addUnitHealth(out,arry[iX][iY].getiID(),iLife);
			if(iRet == 0)// -- death
			{
				func_UnitAnimation(out,typeUnit[arry[iX][iY].getiID()],
						UnitAnimationType.death);
				if(typeUnit[arry[iX][iY].getiID()].getiFunction()==13)
				{
					// -- bug
		    		func_CardListadd(1,0);
		    		func_CardListUpdate(out);
				}
				funcSleep(1000);
				// -- Main monster bleeds out and dies Game ends or unit is deleted
				if(typeUnit[99].getHealth() == 0||typeUnit[98].getHealth() == 0)
				{
					TheOneFunc_Logic.humanPlayer.setHealth(TheOneFunc_Logic.typeUnit[98].getHealth());
					TheOneFunc_Logic.aiPlayer.setHealth(TheOneFunc_Logic.typeUnit[99].getHealth());
					BasicCommands.setPlayer1Health(out, TheOneFunc_Logic.humanPlayer);
					BasicCommands.setPlayer2Health(out, TheOneFunc_Logic.aiPlayer);
					BasicCommands.addPlayer1Notification(out, "Game Over", 200);
					TheOneFunc_Logic.gameStatus = 0;
					displayOver(out);
				}
				else
				{
					BasicCommands.addPlayer1Notification(out, "Unit lost", 2);
					BasicCommands.deleteUnit(out,typeUnit[arry[iX][iY].getiID()]);
					initPositionStatus(out,iX,iY);
				}
				iRet = -1;
			}
			else
			{
				// -- AI Player Main Monster play
				if(arry[iX][iY].getiID() ==99||arry[iX][iY].getiID()  ==98)
				{
					BasicCommands.setPlayer1Health(out, humanPlayer);
					BasicCommands.setPlayer2Health(out, aiPlayer);
				}
				func_UnitAnimation(out,typeUnit[arry[iX][iY].getiID()],
						UnitAnimationType.hit);
	            TheOneFunc_Logic.func_UnitAnimation(out,typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()],
	                    UnitAnimationType.idle);
	            TheOneFunc_Logic.func_UnitAnimation(out,TheOneFunc_Logic.typeUnit[TheOneFunc_Logic.arry[iX][iY].getiID()],
	                    UnitAnimationType.idle);
			}
			return iRet;
	    }
	    
	    // -- update card Add a few cards to your hand
		/*
		 * name: func_CardListadd
		 * using: update card 
		 * input: ActorRef out,int iX,int iY
		 * coder: change MK.L   2023.3.2 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static void func_CardListadd(int iNum,int iNo)
	    {
		    for (int j = 0; j < iNum; j++) {
		        theOnelist.add(theOnelistToAll.get(TheOneConfig.gDeckCardNumAll[0]));
		        TheOneConfig.gDeckCardNumAll[0]++;
		    }
	    }
		/*
		 * name: takeAllCards
		 * using: Take all the cards in your hand game over
		 * input: 
		 * coder: create MK.L   2023.3.8 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static int takeAllCards()
	    {
	    	int iRet = 0;
	    	if(TheOneConfig.gDeckCardNumAll[0]>20)
	    	{
	    		iRet = -1;
	    	}
	    	return iRet;
	    }
	    
	    
	    
	    // -- Click on the card to highlight the function
		/*
		 * name: func_CardHighLight
		 * using: Click on the card to highlight the function
		 * input: ActorRef out,Card cCard,int iPosition
		 * coder: change MK.L   2023.3.2 Xiuqi Liu 2773750
		 * change:
		 */	
	    public static void func_CardHighLight(ActorRef out,Card cCard,int iPosition)
	    {
			for(int i = 1; i <= 6; i++) 
			{
				if(theOnelist.get(i-1) != null)
				{
					BasicCommands.drawCard(out, theOnelist.get(i-1), i, 0);
				}
				
			}
			
			BasicCommands.drawCard(out, cCard, iPosition, 1);
			TheOneConfig.iCardNeedToPut = iPosition;
	    }

		/*
		 * name: func_CardHighLight
		 * using: Update the blue amount when the card is played
		 * input: ActorRef out, Player player,Card card,int iposition
		 * coder: change luo 2.27 2023
		 * change:
		 */	
		public static void updateManaWhenPutCard(ActorRef out, Player player,Card card,int iposition)
		{
			int manaCost = theOnelist.get(iposition-1).getManacost();
			if(player.getMana() >= manaCost)
			{
				player.setMana(player.getMana()-manaCost);
			}
	
		}

		/*
		 * name: updateMana
		 * using: Update the blue level at the end of the round 
		 * input: ActorRef out, Player player,int mana,int iType
		 * 0ok 1 overflow
		 * coder: change luo 2.27 2023
		 * change:
		 */	
		public static int updateMana(ActorRef out, Player player,int mana,int iType)
		{
			int iRet = 0;
			if(mana>player.getMana())
			{
				iRet = 1;
			}
			else
			{
				player.setMana(player.getMana()-mana);
				if(iType == 1)// -- player
				{
					BasicCommands.setPlayer1Mana(out, player);
				}
				else if(iType == 2)
				{
					BasicCommands.setPlayer2Mana(out, player);
				}
				
			}
			return iRet;
		}
		/*
		 * name: updateManaWhenEndTurn
		 * using: Update the blue level at the end of the round
		 * input: ActorRef out, Player player
		 *
		 * coder: change luo 2.27 2023
		 * change:
		 */			
		public static void updateManaWhenEndTurn(ActorRef out, Player player)
		{
			// luo Determine whether the current player or AI is a player and whether the blue level is greater than 7
			if(TheOneConfig.iEnTurnClick<=7 && player== getHumanPlayer())
			{
				player.setMana(TheOneConfig.iEnTurnClick+2);
				BasicCommands.setPlayer1Mana(out, player);
			}
			else if(TheOneConfig.iEnTurnClick>7 && player== getHumanPlayer())
			{
				player.setMana(9);
				BasicCommands.setPlayer1Mana(out, player);
			}
			else if(TheOneConfig.iEnTurnClick<=7 && player== getAiPlayer())
			{
				player.setMana(TheOneConfig.iEnTurnClick+2);
				BasicCommands.setPlayer2Mana(out, player);
			}
			else if(TheOneConfig.iEnTurnClick>7 && player== getAiPlayer())
			{
				player.setMana(9);
				BasicCommands.setPlayer2Mana(out, player);
			}
		}
		/*
		 * name: displayMovePosition
		 * using: Click on Unit to display functions with moveable coordinates 
		 * 
		 * input: ActorRef out,int iX,int iY,int iPosion
		 *iPosion 66 移动过
		 * coder: change MK.L   2023.2.20 Xiuqi Liu 2773750
		 * change:luo 2.30 Click on Unit to display functions with moveable coordinates
		 */		
		public static int displayMovePosition(ActorRef out,int iX,int iY,int iPosion)
		{
			int iRet = 0;
			// luo The first for loop initializes each coordinate so that they are all in a low lit state.
				//(to prevent the first unit from still being lit on the second click)
			Tile[][] tiles = publicTiles;
			if(iPosion != 88)
			{
				tiles = func_CreateBoard(tiles,out);
				
			}
			funcSleep(20);
			funcCardCanPutOrNot(iPosion,out,99,99) ;
			if(iX>50||iY>50)
			{
				return -1;
			}
			funcSleep(20);
			// luo Determine if the current coordinate is a unit or player
			if(arry[iX][iY].isUnit() || arry[iX][iY].isPlayer())
			{
				if(iPosion == 66)
				{
					TheOneConfig.clickUnit = typeUnit[arry[iX][iY].getiID()];
					BasicCommands.drawTile(out, tiles[iX][iY], 1);
				}
				else if(typeUnit[arry[iX][iY].getiID()].getiFunction() == 13)// -- special card
				{// -- Global movement
					for(int i = 0;i<5;i++)
					{
						for(int j = 0;j<9;j++)
						{
							if(!arry[j][i].isUnit()&&!arry[j][i].isPlayer())
							{
								tiles[j][i]= BasicObjectBuilders.loadTile(j, i);
								BasicCommands.drawTile(out, tiles[j][i], 1);
							}
							funcSleep(1);
						}
					}
				}
				else
				{
					if(iPosion == 88)
					{
						TheOneFunc_Logic.funcSleep(5);
						TheOneConfig.clickUnit = typeUnit[arry[iX][iY].getiID()];
						for (int i = -1; i < 2; i++)
						{
							// luo Determine if the current horizontal coordinate +2 or -2 is greater than 8 or less than 0 
							//(greater than 8 or less than 0 is out of bounds)
							if(iX+i>=0 && iX+i<=8)
							{
								funcChangeTileStatus(iX+i,iY,tiles,out);
							}
							// luo Determine if the current vertical coordinate +2 or -2 is greater than 4 or less than 0 
							//(greater than 4 or less than 0 is out of bounds)
							if(iY+i>=0 && iY+i<=4)
							{
						
								funcChangeTileStatus(iX,iY + i,tiles,out);
							}
						}
						// luo Determines if the left oblique corner of the current coordinate is out of bounds
						if(iX-1>=0 && iY-1>=0)
						{
							funcChangeTileStatus(iX - 1,iY - 1,tiles,out);
						}
						// luo Determine if the lower right corner of the current coordinate is out of bounds
						if(iX+1<=8 && iY+1<=4)
						{
							funcChangeTileStatus(iX + 1,iY + 1,tiles,out);
						}
						// luo Determine if the lower left corner of the current coordinate is out of bounds
						if (iX-1>=0 && iY+1<=4)
						{
							funcChangeTileStatus(iX - 1,iY + 1,tiles,out);
						}
						// luo Determine if the upper right corner of the current coordinate is out of bounds
						if (iX+1<=8 && iY-1>=0)
						{
							funcChangeTileStatus(iX + 1,iY - 1,tiles,out);
						}
					}
					else
					{
						// -- fix out range place
						for (int j = 0; j < 9; j++)
						{
							for (int i = 0; i < 5; i++)
							{
								TheOneConfig.lowLightPosition[j][i]=0;
							}
							funcSleep(1);
						}
							
						// luo If it's a unit, get the unit and save it in the clickunit
						TheOneConfig.clickUnit = typeUnit[arry[iX][iY].getiID()];
						// luo The for loop is to make the coordinates that can be moved after clicking on the unit become highlighted or in red brightness mode
						// luo The back is full of this judgement
						for (int j = 0; j < 9; j++)
						{
							for (int i = 0; i < 5; i++)
							{
								if(isOneGridAway(j,i,iX,iY) &&
										((arry[j][i].isPlayer()||arry[j][i].isUnit()))
										&&arry[j][i].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
								{
									
									if(typeUnit[arry[j][i].getiID()].getiMocking()==1)
									{
										BasicCommands.addPlayer1Notification(out, "can not move", 20);
										typeUnit[arry[iX][iY].getiID()].setiMove(1);
										return -2;
									}
								}
							}
							funcSleep(1);
						}
						for (int i = -2; i < 3; i++)
						{
							// luo Determine if the current horizontal coordinate +2 or -2 is greater than 8 or less than 0 (greater than 8 or less than 0 is out of bounds)
							if(iX+i>=0 && iX+i<=8)
							{
								if(i == 2 && (arry[iX+1][iY].isUnit() || arry[iX+1][iY].isPlayer())&&
										arry[iX+1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
								{
									TheOneConfig.lowLightPosition[iX + i][iY]=0;
								}
								else if(i == -2 && (arry[iX-1][iY].isUnit() || arry[iX-1][iY].isPlayer())&&
										arry[iX-1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
								{
									TheOneConfig.lowLightPosition[iX + i][iY]=0;
								}else
								{
									funcChangeTileStatus(iX+i,iY,tiles,out);
								}
							}
							// luo Determine if the current vertical coordinate +2 or -2 is greater than 4 or less than 0 (greater than 4 or less than 0 is out of bounds)
							if(iY+i>=0 && iY+i<=4)
							{
								if(i == 2 && (arry[iX][iY+1].isUnit() || arry[iX][iY+1].isPlayer())&&
										arry[iX][iY+1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
								{
									TheOneConfig.lowLightPosition[iX][iY+i]=0;
								}
								else if(i == -2 && (arry[iX][iY-1].isUnit() || arry[iX][iY-1].isPlayer())&&
										arry[iX][iY-1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
								{
									TheOneConfig.lowLightPosition[iX][iY+i]=0;
								}else
								{
									funcChangeTileStatus(iX,iY + i,tiles,out);
								}
							}

						}
						// luo Determines if the left oblique corner of the current coordinate is out of bounds
						if(iX-1>=0 && iY-1>=0)
						{
							if((arry[iX][iY-1].isUnit() || arry[iX][iY-1].isPlayer())&&
									arry[iX][iY-1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution()&&
									(arry[iX-1][iY].isUnit() || arry[iX-1][iY].isPlayer())&&
									arry[iX-1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
							{
								TheOneConfig.lowLightPosition[iX-1][iY-1]=0;
							}
							else
							{
								funcChangeTileStatus(iX - 1,iY - 1,tiles,out);
							}
							
						}
						// luo Determine if the lower right corner of the current coordinate is out of bounds
						if(iX+1<=8 && iY+1<=4)
						{
							if((arry[iX][iY+1].isUnit() || arry[iX][iY+1].isPlayer())&&
									arry[iX][iY+1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution()&&
									(arry[iX+1][iY].isUnit() || arry[iX+1][iY].isPlayer())&&
									arry[iX+1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
							{
								TheOneConfig.lowLightPosition[iX+1][iY+1]=0;
							}
							else
							{
								funcChangeTileStatus(iX + 1,iY + 1,tiles,out);
							}
						}
						// luo Determine if the lower left corner of the current coordinate is out of bounds
						if (iX-1>=0 && iY+1<=4)
						{
							if((arry[iX][iY+1].isUnit() || arry[iX][iY+1].isPlayer())&&
									arry[iX][iY+1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution()&&
									(arry[iX-1][iY].isUnit() || arry[iX-1][iY].isPlayer())&&
									arry[iX-1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
							{
								TheOneConfig.lowLightPosition[iX+1][iY+1]=0;
							}
							else
							{
								funcChangeTileStatus(iX - 1,iY + 1,tiles,out);
							}
						}
						// luo Determine if the upper right corner of the current coordinate is out of bounds
						if (iX+1<=8 && iY-1>=0)
						{
							if((arry[iX][iY-1].isUnit() || arry[iX][iY-1].isPlayer())&&
									arry[iX][iY-1].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution()&&
									(arry[iX+1][iY].isUnit() || arry[iX+1][iY].isPlayer())&&
									arry[iX+1][iY].getSquareStatus() != typeUnit[arry[iX][iY].getiID()].getiAttribution())
							{
								TheOneConfig.lowLightPosition[iX-1][iY-1]=0;
							}
							else
							{
								funcChangeTileStatus(iX + 1,iY - 1,tiles,out);
							}
						}
					}
				}

			}
			return iRet;
		}

			
		/*
		 * name: displayOver
		 * using: End of show
		 * input: ActorRef out
		 *iPosion 66 Move over
		 * coder: change MK.L   2023.3.5 Xiuqi Liu 2773750
		 * change:
		 */		
		public static int displayOver(ActorRef out)
		{
			int iRet = 0;
			int iType = 0;
			while(iRet < 10)
			{
				Tile[][] tiles = publicTiles;
				for(int i = 0;i<5;i++)
				{
					for(int j = 0;j<9;j++)
					{
		
						tiles[j][i]= BasicObjectBuilders.loadTile(j, i);
						BasicCommands.drawTile(out, tiles[j][i], iType);
						funcSleep(1);
					}
				}
				funcSleep(500);
				if(iType == 1)
				{
					iType = 2;
				}else if(iType == 2)
				{
					iType = 0;
				}
				else if(iType == 0)
				{
					iType = 1;
				}
				iRet++;
			}
			return iRet;
		}

		/*
		 * name: funcChangeTileStatus
		 * using: Change tile colour
		 * input: int iPsX,int iPsY,Tile[][] tiles,ActorRef out
		 *iPosion 66 Move over
		 * coder: remake from code MK.L   2023.2.20 Xiuqi Liu 2773750
		 * change:
		 */		
		public static void funcChangeTileStatus(int iPsX,int iPsY,Tile[][] tiles,ActorRef out)
		{
			// luo If not out of bounds, change the coordinate mode in the top left corner to highlight
			if(arry[iPsX][iPsY].isUnit() || arry[iPsX][iPsY].isPlayer())
			{
				if(arry[iPsX][iPsY].getSquareStatus() == 1)
				{
					// -- Low light
					BasicCommands.drawTile(out, tiles[iPsX][iPsY], 0);
				}
				else if(arry[iPsX][iPsY].getSquareStatus() == 2)
				{
					// luo Determine the current coordinates, whether there is a UNIT, and if so, change to red brightness mode
					BasicCommands.drawTile(out, tiles[iPsX][iPsY], 2);
				}
				// luo Set this coordinate to be unmovable
				TheOneConfig.lowLightPosition[iPsX][iPsY]=0;
			}
			else
			{
				// luo If there is no unit, then switch to highlight mode
				BasicCommands.drawTile(out, tiles[iPsX][iPsY], 1);
				// luo Set this coordinate to be movable	
				TheOneConfig.lowLightPosition[iPsX][iPsY]=1;
			}
			
		}
		/*
		 * name: funcSquareStatusUpdate
		 * using: Update square status after each move
		 * 	Placement card unit placed inside square 2.20 enemy status 0 human 1 ai
		 * input: ActorRef out,int iX,int iY,int iID,int status,boolean bIsPlayer
		 *
		 * coder: remake from code MK.L   2023.2.20 Xiuqi Liu 2773750
		 * change:
		 */		
		public static void funcSquareStatusUpdate(ActorRef out,int iX,int iY,int iID,int status,boolean bIsPlayer)
		{
			arry[iX][iY].setPlayer(bIsPlayer);
			arry[iX][iY].setiID(iID);
			arry[iX][iY].setSquareStatus(status); 
			arry[iX][iY].setUnit(!bIsPlayer) ;
			arry[iX][iY].setSquareStatus(status);
		}
		/*
		 * name: initPositionStatus
		 * using: Initialising coordinate positions
		 * input: ActorRef out,int iX,int iY
		 * coder: remake from code MK.L   2023.2.20 Xiuqi Liu 2773750
		 * change:
		 */		
		public static void initPositionStatus(ActorRef out,int iX,int iY)
		{
			if(arry[iX][iY].isPlayer())
			{
				arry[iX][iY].setPlayer(false);
			}
			else if(arry[iX][iY].isUnit())
			{
				arry[iX][iY].setUnit(false);
			}
			arry[iX][iY].setSquareStatus(0);
			arry[iX][iY].setiID(0);
		}


	// luo Two functions to get the player and AI
	public static Player getHumanPlayer() {
		return humanPlayer;
	}

	public static Player getAiPlayer() {
		return aiPlayer;
	}
	// -- standby
	public static Player funcCardfunction() {
		return aiPlayer;
	}    
	

	/*
	 * name: funcUnitCanAttackOrNot
	 * using: Determining whether a unit can be attacked or not
	 * input: int iPosion,ActorRef out,int iX,int iY
	 * unit can attack -1 can place failed iPosion 66 cannot attack 88 check 55 attack point check
	 * 55 attack with provocation not provocation unit -2 attack with provocation provocation unit -1 no provocation 0
	 * 77 atack counterattack check
	 * coder: remake from code MK.L   2023.2.20 Xiuqi Liu 2773750
	 * change:
	 */		
	public static int funcUnitCanAttackOrNot(int iPosion,ActorRef out,int iX,int iY) 
	{
		
		int iRet = 0;
		if(iPosion == 66)
		{
			return 66;
		}

		iRet =	typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()].getiFunction();
		Tile[][] tiles = publicTiles;
		if(iRet == 11||iRet == 5)
		{

			if(iPosion == 77)
			{
				
			}
			else
			{
				// -- 8 units around your unit
				for (int j = -1; j <= 1; j++)
				{
					for (int i = -1; i <= 1; i++)
					{
						if(iX+j<0||iY+i<0||iX+j>=8||iY+i>=4)// change from 9 5 to 8 4
						{
							continue;
						}
						funcSleep(5);
						if(arry[iX+j][iY+i]!=null)
						{
							if(TheOneConfig.clickPosition[0]+j<0||TheOneConfig.clickPosition[1]+i<0
									||TheOneConfig.clickPosition[0]+j>=8||TheOneConfig.clickPosition[1]+i>=4)//从 9 5 改到 8 4
							{
								continue;
							}
							if(iPosion == 55)
							{
								if((arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].isPlayer()||
										arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].isUnit())&&
										arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].getSquareStatus() !=
												typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()].getiAttribution()&&
										// -- And with provocative units
										typeUnit[arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].getiID()].getiMocking() == 1)
								{
									iRet = -3;
								}
								else
								{
									continue;
								}
								// -- If the point of attack is a provoking unit
								if((arry[iX][iY].isPlayer()||arry[iX][iY].isUnit())&&
										arry[iX][iY].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
												.getiID()].getiAttribution()&&typeUnit[arry[iX][iY].getiID()].getiMocking() == 1)
								{
									BasicCommands.addPlayer1Notification(out, "mocking", 2);
									return -1;
								}
								else if((arry[iX][iY].isPlayer()||arry[iX][iY].isUnit())&&
										arry[iX][iY].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
												.getiID()].getiAttribution()&&typeUnit[arry[iX][iY].getiID()].getiMocking() != 1)// -- 如果攻击点不是挑衅单位
								{
									BasicCommands.addPlayer1Notification(out, "Wrong place click other place to cancel", 2);
									BasicCommands.drawTile(out, tiles[iX][iY], 0);

									return -3;
								}
							}
							else if((arry[iX+j][iY+i].isPlayer()||arry[iX+j][iY+i].isUnit())&&
									arry[iX+j][iY+i].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
											.getiID()].getiAttribution()&&typeUnit[arry[iX+j][iY+i].getiID()].getiMocking() == 1)
							{

								BasicCommands.drawTile(out, tiles[iX+j][iY+i], 2);
								return -1;
							}
						}
					}
				}
				TheOneFunc_Logic.funcSleep(5);
				// -- Full map attack
				for (int j = 0; j < 9; j++)
				{
					for (int i = 0; i < 5; i++)
					{
						if((arry[j][i].isPlayer()||arry[j][i].isUnit())&&
								arry[j][i].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
										.getiID()].getiAttribution())
						{
							
							BasicCommands.drawTile(out, tiles[j][i], 2);
							funcSleep(5);
						}
					}
					funcSleep(2);
				}
				iRet = -1;
			}
			
			
		}
		else
		{
			
			// -- 8 units around your unit
			for (int j = -1; j <= 1; j++)
			{
				for (int i = -1; i <= 1; i++)
				{
					if(iX+j<0||iY+i<0||iX+j>=8||iY+i>=4)//Change from 9 5 to 8 4
					{
						continue;
					}
					funcSleep(5);
					if(arry[iX+j][iY+i]!=null)
					{	
						if(TheOneConfig.clickPosition[0]+j<0||TheOneConfig.clickPosition[1]+i<0
								||TheOneConfig.clickPosition[0]+j>=8||TheOneConfig.clickPosition[1]+i>=4)//从 9 5 改到 8 4
						{
							continue;
						}
						if(iPosion == 55)
						{
							if((arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].isPlayer()||
									arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].isUnit())&&
									arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].getSquareStatus() != 
									typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]].getiID()].getiAttribution()&&
									// -- And with provocative units
									typeUnit[arry[TheOneConfig.clickPosition[0]+j][TheOneConfig.clickPosition[1]+i].getiID()].getiMocking() == 1)
							{
								iRet = -3;
							}
							else
							{
								continue;
							}
							// -- If the point of attack is a provoking unit
							if((arry[iX][iY].isPlayer()||arry[iX][iY].isUnit())&&
									arry[iX][iY].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
											.getiID()].getiAttribution()&&typeUnit[arry[iX][iY].getiID()].getiMocking() == 1)
							{
								BasicCommands.addPlayer1Notification(out, "mocking", 2);
								return -1;
							}
							else if((arry[iX][iY].isPlayer()||arry[iX][iY].isUnit())&&
									arry[iX][iY].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
											.getiID()].getiAttribution()&&typeUnit[arry[iX][iY].getiID()].getiMocking() != 1)// -- 如果攻击点不是挑衅单位
							{
								BasicCommands.addPlayer1Notification(out, "Wrong place click other place to cancel", 2);
								BasicCommands.drawTile(out, tiles[iX][iY], 0);
								
								return -3;
							}
						}
						else if((arry[iX+j][iY+i].isPlayer()||arry[iX+j][iY+i].isUnit())&&
								arry[iX+j][iY+i].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
										.getiID()].getiAttribution()&&typeUnit[arry[iX+j][iY+i].getiID()].getiMocking() == 1)
						{
							
							BasicCommands.drawTile(out, tiles[iX+j][iY+i], 2);
							return -1;
						}
					}
				}
			}
			if(iPosion == 77)
			{
				// -- 8 units around your unit
				for (int j = -1; j <= 1; j++)
				{
					for (int i = -1; i <= 1; i++)
					{
						
						if(iX+j<0||iY+i<0||iX+j>=9||iY+i>=5)
						{
							continue;
						}
						
						if(arry[iX+j][iY+i]!=null)
						{
							if((arry[iX+j][iY+i].isPlayer()||arry[iX+j][iY+i].isUnit())&&
									arry[iX+j][iY+i].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
											.getiID()].getiAttribution())
							{
								//BasicCommands.drawTile(out, tiles[iX+j][iY+i], 2);
								iRet = -1;
							}
						}
					}
					
				}
			}
			else
			{
				// -- 8 units around your unit
				for (int j = -1; j <= 1; j++)
				{
					for (int i = -1; i <= 1; i++)
					{
						
						if(iX+j<0||iY+i<0||iX+j>=9||iY+i>=5)
						{
							continue;
						}
						
						if(arry[iX+j][iY+i]!=null)
						{
							if((arry[iX+j][iY+i].isPlayer()||arry[iX+j][iY+i].isUnit())&&
									arry[iX+j][iY+i].getSquareStatus() != typeUnit[arry[TheOneConfig.clickPosition[0]][TheOneConfig.clickPosition[1]]
											.getiID()].getiAttribution())
							{
								BasicCommands.drawTile(out, tiles[iX+j][iY+i], 2);
								iRet = -1;
							}
						}
					}
					
				}
				TheOneFunc_Logic.funcSleep(5);
			}
		}

		return iRet;
	}   
	
	// -- 卡牌可以放置的地点 -1 可以 放置失败
	public static int funcCardCanPutOrNot(int iPosion,ActorRef out,int iX,int iY) 
	{
		int iRet = -1;
		if(iPosion>6&&iPosion != 77)
		{
			return -2;
		}
		Tile[][] tiles = publicTiles;
		if(iPosion == 77 ||iPosion == 66)
		{
			iRet = compareConfigToFunction(configdata,cID);
		}else
		{
			iRet = theOnelist.get(iPosion - 1).getiFunction();
		}
		
		if(iRet == 16||iRet == 7)
		{
			// -- 敌方unit可放置
			if(TheOneConfig.roundStatus==2)// -- AI
			{
				//arry[iX][iY].getSquareStatus()==1 // -- 敌方
				for (int j = 0; j < 9; j++)
				{
					for (int i = 0; i < 5; i++)
					{
						if(arry[j][i].getSquareStatus()==1)
						{
							if(iPosion == 77)
							{
								if(j == iX&&i == iY)
								{
									return -1;
								}
							}
							else
							{
								BasicCommands.drawTile(out, tiles[j][i], 2);
							}
							
						}
					}
				}
			}else if(TheOneConfig.roundStatus==1)
			{
				//arry[iX][iY].getSquareStatus()==2 // -- 敌方
				for (int j = 0; j < 9; j++)
				{
					for (int i = 0; i < 5; i++)
					{
						if(arry[j][i].getSquareStatus()==2)
						{
							if(iPosion == 77)
							{
								if(j == iX&&i == iY)
								{
									return -1;
								}
							}
							else
							{
								BasicCommands.drawTile(out, tiles[j][i], 2);
							}
						}
					}
				}
				
			}
		}
		else if(iRet == 15||iRet == 8)
		{
			// -- 己方unit可以放置
			if(TheOneConfig.roundStatus==2)// -- AI
			{
				//arry[iX][iY].getSquareStatus()==1 // -- 敌方
				for (int j = 0; j < 9; j++)
				{
					for (int i = 0; i < 5; i++)
					{
						if(arry[j][i].getSquareStatus()==2)
						{
							if(iPosion == 77)
							{
								if(j == iX&&i == iY)
								{
									return -1;
								}
							}
							else
							{
								BasicCommands.drawTile(out, tiles[j][i], 1);
							}
						}
					}
				}
			}else if(TheOneConfig.roundStatus==1)
			{
				//arry[iX][iY].getSquareStatus()==2 // -- 敌方
				for (int j = 0; j < 9; j++)
				{
					for (int i = 0; i < 5; i++)
					{
						if(arry[j][i].getSquareStatus()==1)
						{
							if(iPosion == 77)
							{
								if(j == iX&&i == iY)
								{
									return -1;
								}
							}
							else
							{
								BasicCommands.drawTile(out, tiles[j][i], 1);
							}
							
						}
					}
				}
			}
		}
		else if(iRet == 9||iRet == 6)
		{
			// -- 所有tile（无unit单位）可放置
			//arry[iX][iY].getSquareStatus()==2 // -- 敌方
			for (int j = 0; j < 9; j++)
			{
				for (int i = 0; i < 5; i++)
				{
					if(!arry[j][i].isPlayer()&&!arry[j][i].isUnit())
					{
						BasicCommands.drawTile(out, tiles[j][i], 1);
					}
				}
				funcSleep(2);
			}
			iRet = -1;
		}
		else
		{
			// -- 己方unit周围8个单位
			for (int j = 0; j < 9; j++)
			{
				for (int i = 0; i < 5; i++)
				{
					if(arry[j][i].isPlayer()||arry[j][i].isUnit())
					{
						if((arry[j][i].getSquareStatus()==1&&TheOneConfig.roundStatus==1)
							||(arry[j][i].getSquareStatus()==2&&TheOneConfig.roundStatus==2))
						{
							if((Math.sqrt((j - iX) * (j - iX) + (i - iY) * (i - iY))<=2)
									&& (i - iY)*(i - iY)<4&&(j - iX) * (j - iX)<4)
							{
								return -1;
							}
							else 
							{
								
								displayMovePosition(out,j,i,88);
								funcSleep(5);
							}
						}
						
					}
				}
			}
		}
		
		return iRet;
	}   
	/*
	 * 0:no function
	 * 1: If the enemy plays a spell token, that UNIT has +1 attack and +1 blood
	 * 2: +3 to player blood when that unit is summoned (player blood maxes out at 20)
	 * 3: 1. Our unit provokes (provocateur): if an enemy unit can attack and is adjacent to any provocateur (8 directions)
	 * then this enemy UNIT can only choose to attack the provocateur. The enemy unit cannot move while being provoked (i.e. only if the enemy unit reduces the blood of our provoker's unit to 0
	 * Can only move and choose another unit to attack) 2. If our player is attacked, that unit's attack is +2
	 * 4: You can attack the enemy 2 times per round
	 * 5: The range of the attack can be any enemy position on the board (i.e. it does not have to be adjacent to each other to attack)
	 * 6: 1. The unit can be placed anywhere on the board (normal placement of the unit is adjacent to our player and the unit)
	 * 2. Our unit provokes (provocateur): If an enemy unit can attack and is adjacent to any provocateur (8 directions)
	 * then this enemy unit can only choose to attack the provocateur. The enemy unit cannot move while being provoked (i.e. only if the enemy unit reduces the blood of our provoker's unit to 0
	 * can only move and choose another unit to attack)
	 * 7: Gives an enemy unit -2 blood
	 * 8: Gives a unit +5 to its blood (but not more than the initial blood of that unit)
	 * 
	 * 9: That unit can be placed anywhere on the board
	 * 10: Our unit provokes (provocateur): if an enemy unit can attack and is adjacent to any provocateur (8 directions)
	 * then this enemy UNIT can only choose to attack the provocateur. The enemy unit cannot move while being provoked (i.e. only if the enemy unit reduces the blood of our provoker's unit to 0
	 * can only move and choose another unit to attack)
	 * 11: The range of the attack can be any enemy position on the board (i.e. you don't have to be adjacent to it to attack)
	 * 12: When this unit is summoned, both players draw one card each
	 * 13: 1. The unit may move anywhere on the board
	 * 2. When the unit is at 0 blood, that player draws a card
	 * 14: You may attack the enemy 2 times per turn
	 * 15: Make that player's attack +2
	 * 16: Reduces the blood of a unit other than a player to 0
	 */

	/*
	 * name: funcCardSetOutput
	 * using: Input function output result
	 * input: (ActorRef out,int iFunction,int iX,int iY
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int funcOutput(ActorRef out,int iFunction,int iX,int iY) 
	{
		int iRet = 0;
		if(iFunction == 0)
		{
			// -- null
		}else if(iFunction == 1)
		{
			// -- 如果敌方出spell牌，该unit的攻击力+1，血量+1
			// -- 设置unit血量魔法	
			//funcSleep(50);
			addUnitAttack(out,arry[iX][iY].getiID(),typeUnit[arry[iX][iY].getiID()].getiAttack()+1);
			addUnitHealth(out,arry[iX][iY].getiID(),typeUnit[arry[iX][iY].getiID()].getHealth()+1);
			
		}else if(iFunction == 3)
		{
			// -- 1.我方unit挑衅(挑衅者)：如果敌方某unit能攻击且与任何挑衅者相邻(8个方向)，
			// -- 那么这个敌方unit只能选择攻击挑衅者。敌方unit在被挑衅时不能移动(即只有敌方unit把我方挑衅者unit的血量减为0时，
			// -- 才能移动和选择其他的unit攻击)2.如果我方玩家受到攻击，则该unit的攻击力+2
			
		}else if(iFunction == 6)
		{
			// -- 6：1.该unit可以放在棋盘上的任意位置(正常unit的放置位置要与我方的玩家和unit相邻)
			// -- 2.我方unit挑衅(挑衅者)：如果敌方某unit能攻击且与任何挑衅者相邻(8个方向)，
		}else if(iFunction == 10)
		{
			// -- 10：我方unit挑衅(挑衅者)：如果敌方某unit能攻击且与任何挑衅者相邻(8个方向)，
			// -- 那么这个敌方unit只能选择攻击挑衅者。敌方unit在被挑衅时不能移动(即只有敌方unit把我方挑衅者unit的血量减为0时，
			// -- 才能移动和选择其他的unit攻击)
		}else if(iFunction == 12)
		{
			// -- 当召唤该unit时，双方玩家各抽一张牌
		}else if(iFunction == 13)
		{
			// -- 1.该unit可以在棋盘上任意移动 2.当该unit血量为0时，该方玩家抽一张牌
		}
		
		return iRet;
	}   
	/*
	 * name: funcCardSetOutput
	 * using: The features that come with this card
	 * input: ActorRef out,int iFunction,int iX,int iY
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */	
	public static int funcCardSetOutput(ActorRef out,int iFunction,int iX,int iY) 
	{
		int iRet = -1;
		if(iFunction == 0)
		{
			// -- null
		}else if(iFunction == 1)
		{
			// -- If the enemy plays a spell tile, the unit has +1 attack and +1 blood
			// -- Set the unit's blood magic	
			// -- spell placement implementation
			//funcSleep(50);
			if(TheOneConfig.spellStatus == 1)
			{
				
				TheOneConfig.spellStatus = 0;
			}
		}else if(iFunction == 2)
		{
			// -- 2：+3 to player blood when this UNIT is summoned (player blood maxes out at 20)
			int health = 0;
			funcSleep(5);
			if(TheOneConfig.roundStatus==1)
			{
				if((health = typeUnit[98].getHealth()+3)>=20)
				{
					health = 20;
				}
				addUnitHealth(out,98,health);
				typeUnit[98].setHealth(health);
			}else if(TheOneConfig.roundStatus==2)
			{
				if((health = typeUnit[99].getHealth()+3)>=20)
				{
					health = 20;
				}
				addUnitHealth(out,99,health);
				typeUnit[99].setHealth(health);
			}
			
		}else if(iFunction == 3)
		{
			// -- 1. Our unit provokes (provocateur): if an enemy unit can attack and is adjacent to any provocateur (8 directions)
			// -- then this enemy unit can only choose to attack the provocateur. The enemy unit cannot move while being provoked (i.e. only if the enemy unit reduces our provoker unit's blood to 0
			// -- can move and choose another unit to attack) 2. If our player is attacked, that unit's attack is +2
			typeUnit[arry[iX][iY].getiID()].setiMocking(1);
			
		}else if(iFunction == 4)
		{
			// -- 4：You can attack the enemy 2 times per round
		}else if(iFunction == 5)
		{
			// -- 5：The range of the attack can be any enemy position on the board (i.e. you don't have to be adjacent to it to attack)
		}else if(iFunction == 6)
		{
			typeUnit[arry[iX][iY].getiID()].setiMocking(1);
			// -- 6: 1. The unit may be placed anywhere on the board (normal units are placed adjacent to our player and unit)
			// -- 2. Our unit provokes (provocateur): If an enemy unit can attack and is adjacent to any provocateur (8 directions)
		}else if(iFunction == 7)
		{
			// -- 7：Causes an enemy UNIT to bleed -2
			int health = 0;
			funcSleep(5);
			if(typeUnit[arry[iX][iY].getiID()] == null)
			{
				iRet = -1;
			}
			else
			{
				if((health = typeUnit[arry[iX][iY].getiID()].getHealth()-2)<=0)
				{
					health = 0;
				}
				addUnitHealth(out,arry[iX][iY].getiID(),health);
				typeUnit[arry[iX][iY].getiID()].setHealth(health);
				if(health == 0)
				{
					func_UnitAnimation(out,typeUnit[arry[iX][iY].getiID()],
							UnitAnimationType.death);
		
					funcSleep(1000);
					if(typeUnit[99].getHealth() == 0||typeUnit[98].getHealth() == 0)
					{
						TheOneFunc_Logic.humanPlayer.setHealth(TheOneFunc_Logic.typeUnit[98].getHealth());
						TheOneFunc_Logic.aiPlayer.setHealth(TheOneFunc_Logic.typeUnit[99].getHealth());
						BasicCommands.setPlayer1Health(out, TheOneFunc_Logic.humanPlayer);
						BasicCommands.setPlayer2Health(out, TheOneFunc_Logic.aiPlayer);
						BasicCommands.addPlayer1Notification(out, "Game Over", 200);
						TheOneFunc_Logic.gameStatus = 0;
						displayOver(out);
					}
					else
					{
						BasicCommands.addPlayer1Notification(out, "deleteUnit", 2);
						BasicCommands.deleteUnit(out,typeUnit[arry[iX][iY].getiID()]);
						initPositionStatus(out,iX,iY);
					}
				
				}
			}
		}else if(iFunction == 8)
		{
			// -- 8：Gives a unit +5 to its blood (but not more than the initial blood of that unit)
			int health = 0;
			funcSleep(5);
			if(TheOneConfig.roundStatus==1)
			{
				if((health = typeUnit[arry[iX][iY].getiID()].getHealth()+5)>=typeUnit[arry[iX][iY].getiID()].getBlood())
				{
					health = typeUnit[arry[iX][iY].getiID()].getBlood();
				}
				addUnitHealth(out,arry[iX][iY].getiID(),health);
				typeUnit[arry[iX][iY].getiID()].setHealth(health);
			}else if(TheOneConfig.roundStatus==2)
			{
				if((health = typeUnit[arry[iX][iY].getiID()].getHealth()+5)>=typeUnit[arry[iX][iY].getiID()].getBlood())
				{
					health = typeUnit[arry[iX][iY].getiID()].getBlood();
				}
				addUnitHealth(out,arry[iX][iY].getiID(),health);
				typeUnit[arry[iX][iY].getiID()].setHealth(health);
			}
		}else if(iFunction == 9)
		{
			// -- 9：The unit can be placed anywhere on the board
		}else if(iFunction == 10)
		{
			// -- 10: Our unit provokes (provocateur): if an enemy unit can attack and is adjacent to any provocateur (8 directions)
			// -- then this enemy UNIT can only choose to attack the provocateur. The enemy unit cannot move while being provoked (i.e. only if the enemy unit reduces our provoker unit's blood to 0
			// -- can move and choose another unit to attack)
			typeUnit[arry[iX][iY].getiID()].setiMocking(1);
		}else if(iFunction == 11)
		{
			// -- The range of the attack can be any enemy position on the board (i.e. you don't have to be adjacent to it to attack)
		}else if(iFunction == 12)
		{
    		func_CardListadd(1,0);
    		func_CardListUpdate(out);
			// -- When the unit is summoned, each player draws a card
		}else if(iFunction == 13)
		{
			// -- 1.The unit may move anywhere on the board 2. When the unit is at 0 blood, that player draws a card
		}else if(iFunction == 14)
		{
			// -- 14：You can attack the enemy 2 times per round
		}else if(iFunction == 15)
		{
			// -- 15：Causes the player on that side to attack +2
			int attack = 0;
			funcSleep(5);
			if(typeUnit[arry[iX][iY].getiID()] == null)
			{
				iRet = -1;
			}
			else
			{
				attack = typeUnit[arry[iX][iY].getiID()].getiAttack()+2;
				addUnitAttack(out,arry[iX][iY].getiID(),attack);
				typeUnit[arry[iX][iY].getiID()].setiAttack(attack);
			}
		}else if(iFunction == 16)
		{
			// -- 16：Reduces the blood of a unit other than the player to 0
			int health = 0;
			funcSleep(5);
			if(typeUnit[arry[iX][iY].getiID()] == null||arry[iX][iY].getiID() == 99||arry[iX][iY].getiID() == 98)
			{
				iRet = -1;
			}
			else
			{
				addUnitHealth(out,arry[iX][iY].getiID(),health);
				typeUnit[arry[iX][iY].getiID()].setHealth(health);
				if(health == 0)
				{
					func_UnitAnimation(out,typeUnit[arry[iX][iY].getiID()],
							UnitAnimationType.death);
		
					funcSleep(1000);
					BasicCommands.addPlayer1Notification(out, "deleteUnit", 2);
					BasicCommands.deleteUnit(out,typeUnit[arry[iX][iY].getiID()]);
					initPositionStatus(out,iX,iY);
				}
			}
		}
		
		return iRet;
	}   
	/*
	 * name: funcPureblade_Enforcer
	 * using: Implements the functionality of the Pureblade Enforcer card, which increases the attack and health of holy units on the game board by 1.
	 * input: ActorRef out - an actor reference for sending messages to the game engine, int iFunction - the function of the card, int iX - the x coordinate of the card on the game board, int iY - the y coordinate of the card on the game board.
	 * coder: MK.L   2023.2.7 Xiuqi Liu 2773750
	 * change:
	 */
	// This function represents the functionality of the Pureblade Enforcer card.
	public static int funcPureblade_Enforcer(ActorRef out)
	{
	    // Set spell status to 1 to indicate that a spell is being used.
	    TheOneConfig.spellStatus = 1;

	    // Iterate over all units except for the hero unit (whose ID is 1).
	    for(int i = 2; i < TheOneConfig.iUnitID; i++)
	    {   
	        // Check if the unit at the current index exists.
	        if(typeUnit[i] != null)
	        {
	            // Check if the unit's function is equal to 1 (i.e., the unit is a holy unit).
	            if (typeUnit[i].getiFunction() == 1)
	            {
	                // Increase the unit's attack and health by 1.
	                typeUnit[i].setiAttack(typeUnit[i].getiAttack() + 1);
	                typeUnit[i].setHealth(typeUnit[i].getHealth() + 1);
	                
	                // Update the unit's attack and health on the game board.
	                addUnitAttack(out, i, typeUnit[i].getiAttack());
	                addUnitHealth(out, i, typeUnit[i].getHealth());
	            }
	        }
	        else
	        {
	            // Notify the player if the unit does not exist.
	            BasicCommands.addPlayer1Notification(out, "UID = " + i, 2);
	        }
	    }

	    // Check if the current player's hero unit is the Pureblade Enforcer or the enemy's hero unit.
	    if(TheOneConfig.iUnitID == 98 || TheOneConfig.iUnitID == 99)
	    {
	        // Add additional functionality for the Pureblade Enforcer or the enemy's hero unit.
	    }

	    // Set spell status back to 0 to indicate that the spell is no longer being used.
	    TheOneConfig.spellStatus = 0;
	    return 0;
	}

	/*
	 * name: funcSilverguard_Knight
	 * using: Implements the functionality of the Silverguard Knight card, which increases the attack of allied units on the game board that have a guard ability by 2.
	 * input: ActorRef out - an actor reference for sending messages to the game engine.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int funcSilverguard_Knight(ActorRef out)
	{
	    // Set spell status to 1 to indicate that a spell is being used.
	    TheOneConfig.spellStatus = 1;

	    // Iterate over all units except for the hero unit (whose ID is 1).
	    for(int i = 2; i < TheOneConfig.iUnitID; i++)
	    {   
	        // Check if the unit at the current index exists.
	        if(typeUnit[i] != null)
	        {
	            // Check if the unit's function is equal to 3 (i.e., the unit has a guard ability).
	            if (typeUnit[i].getiFunction() == 3)
	            {
	                // Increase the unit's attack by 2.
	                typeUnit[i].setiAttack(typeUnit[i].getiAttack() + 2);
	                
	                // Update the unit's attack on the game board.
	                addUnitAttack(out, i, typeUnit[i].getiAttack());
	            }
	        }
	        else
	        {
	            // Notify the player if the unit does not exist.
	            BasicCommands.addPlayer1Notification(out, "UID = " + i, 2);
	        }
	    }

	    // Check if the current player's hero unit is the Silverguard Knight or the enemy's hero unit.
	    if(TheOneConfig.iUnitID == 98 || TheOneConfig.iUnitID == 99)
	    {
	        // Add additional functionality for the Silverguard Knight or the enemy's hero unit.
	    }

	    // Set spell status back to 0 to indicate that the spell is no longer being used.
	    TheOneConfig.spellStatus = 0;
	    return 0;
	}

	/*
	 * name: compareAiConfigToiType
	 * using: Determines if the AI's card selection is a unit or a spell.
	 * input: String strConfig - the configuration string of the AI's selected card, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating if the AI's card selection is a unit or a spell.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToiType(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's card type.
	            iRet = theOneAilist.get(i).getiType();
	        }
	    }

	    // Return the card type value.
	    return iRet;
	}


	/*
	 * name: compareAiConfigToAttack
	 * using: Determines the attack value of the AI's selected unit.
	 * input: String strConfig - the configuration string of the AI's selected unit, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating the attack value of the AI's selected unit.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToAttack(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's selected unit's attack value.
	            iRet = theOneAilist.get(i).getiAttack();
	        }
	    }

	    // Return the attack value.
	    return iRet;
	}


	/*
	 * name: compareAiConfigToHealth
	 * using: Determines the health value of the AI's selected unit.
	 * input: String strConfig - the configuration string of the AI's selected unit, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating the health value of the AI's selected unit.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToHealth(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's selected unit's health value.
	            iRet = theOneAilist.get(i).getiHealth();
	        }
	    }

	    // Return the health value.
	    return iRet;
	}


	/*
	 * name: compareAiConfigToiAttribution
	 * using: Determines the attribution value of the AI's selected unit.
	 * input: String strConfig - the configuration string of the AI's selected unit, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating the attribution value of the AI's selected unit.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToiAttribution(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's selected unit's attribution value.
	            iRet = theOneAilist.get(i).getiAttribution();
	        }
	    }

	    // Return the attribution value.
	    return iRet;
	}


	/*
	 * name: compareAiConfigToFunction
	 * using: Determines the function value of the AI's selected unit.
	 * input: String strConfig - the configuration string of the AI's selected unit, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating the function value of the AI's selected unit.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToFunction(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's selected unit's function value.
	            iRet = theOneAilist.get(i).getiFunction();
	        }
	    }

	    // Return the function value.
	    return iRet;
	}


	/*
	 * name: compareAiConfigToMana
	 * using: Determines the mana cost of the AI's selected unit.
	 * input: String strConfig - the configuration string of the AI's selected unit, int ID - the ID of the AI player.
	 * output: int iRet - the integer value indicating the mana cost of the AI's selected unit.
	 * coder: MK.L   2023.3.7 Xiuqi Liu 2773750
	 * change:
	 */
	public static int compareAiConfigToMana(String strConfig, int ID) {
	    // Initialize the return value to 99.
	    int iRet = 99;

	    // Iterate over the AI list to find a match for the given configuration string and ID.
	    for (int i = 0; i < 20; i++) {
	        if (strConfig.equals(theOneAilist.get(i).getUnitConfig()) && ID == theOneAilist.get(i).getId())
	        {
	            // If there is a match, set the return value to the AI's selected unit's mana cost.
	            iRet = theOneAilist.get(i).getManacost();
	        }
	    }

	    // Return the mana cost.
	    return iRet;
	}


	
}

