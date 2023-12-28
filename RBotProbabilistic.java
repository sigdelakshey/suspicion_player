import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/** This is the base class for computer player/bots. 
 * 
 */

public class RBotProbabilistic extends Bot
{
    Random r = new Random();
    HashMap<String, Piece> pieces; // Keyed off of guest name
    Board board;
    Piece me;
    HashMap<String, Player> players; // Keyed off of player name
    String otherPlayerNames[];
    TextDisplay display;

    int[] gemCounts = new int[3];

    public static class Board
    {
        public Room rooms[][];
        public String gemLocations;

        public class Room
        {
            public final boolean gems[] = new boolean[3];
            public final String[] availableGems;
            public final int row;
            public final int col;
            private HashMap<String, Piece> pieces;

            public void removePlayer(Piece piece)
            {
                removePlayer(piece.name);
                piece.col=-1;
                piece.row=-1;
            }

            public void removePlayer(String name)
            {
                pieces.remove(name);
            }
            
            public void addPlayer(Piece piece)
            {
                piece.col=this.col;
                piece.row=this.row;
                pieces.put(piece.name, piece);
            }

            public Room(boolean red, boolean green, boolean yellow, int row, int col)
            {
                pieces = new HashMap<String, Piece>();
                this.row = row;
                this.col = col;
                gems[Suspicion.RED]=red;
                gems[Suspicion.GREEN]=green;
                gems[Suspicion.YELLOW]=yellow;
                String temp="";
                if(red) temp += "red,";
                if(green) temp += "green,";
                if(yellow) temp += "yellow,";
                availableGems = (temp.substring(0,temp.length()-1)).split(",");
            }
        }

        public void movePlayer(Piece player, int row, int col)
        {
            rooms[player.row][player.col].removePlayer(player);
            rooms[row][col].addPlayer(player);
        }
        
        public void clearRooms()
        {
            rooms=new Room[3][4];
            int x=0, y=0;
            boolean red, green, yellow;
        
            for(String gems:gemLocations.trim().split(":"))
            {
                if(gems.contains("red")) red=true;
                else red=false;
                if(gems.contains("green")) green=true;
                else green=false;
                if(gems.contains("yellow")) yellow=true;
                else yellow=false;
                rooms[x][y] = new Room(red,green,yellow,x,y);
                y++;
                x += y/4;
                y %= 4;
            }
        }

        public Board(String piecePositions, HashMap<String, Piece> pieces, String gemLocations)
        {
            Piece piece;
            this.gemLocations=gemLocations;
            clearRooms();
            int col=0;
            int row=0;
            for(String room:piecePositions.split(":",-1)) // Split out each room
            {
                room = room.trim();
                if(room.length()!=0) for(String guest: room.split(",")) // Split guests out of each room
                {
                    guest = guest.trim();
                    piece = pieces.get(guest);
                    rooms[row][col].addPlayer(piece);
                }
                col++;
                row = row + col/4;
                col = col%4;
            }
        }
    }

    public Piece getPiece(String name)
    {
        return pieces.get(name);
    }

    public class Player
    {
        public String playerName;
        public ArrayList<String> possibleGuestNames;
        
        public void adjustKnowledge(ArrayList<String> possibleGuests)
        {
            Iterator<String> it = possibleGuestNames.iterator();
            while(it.hasNext())
            {
                String g;
                if(!possibleGuests.contains(g=it.next())) 
                {
                    it.remove();
                }
            }
        }

        public void adjustKnowledge(String notPossibleGuest)
        {
            Iterator<String> it = possibleGuestNames.iterator();
            while(it.hasNext())
            {
                if(it.next().equals(notPossibleGuest)) 
                {
                    it.remove();
                    break;
                }
            }
        }

        public Player(String name, String[] guests)
        {
            playerName = name;
            possibleGuestNames = new ArrayList<String>();
            for(String g: guests)
            {
                possibleGuestNames.add(g);
            }
        }
    }

    public class Piece
    {
        public int row, col;
        public String name;

        public Piece(String name)
        {
            this.name = name;
        }
    }

    private String[] getPossibleMoves(Piece p)
    {
        LinkedList<String> moves=new LinkedList<String>();
        if(p.row > 0) moves.push((p.row-1) + "," + p.col);
        if(p.row < 2) moves.push((p.row+1) + "," + p.col);
        if(p.col > 0) moves.push((p.row) + "," + (p.col-1));
        if(p.col < 3) moves.push((p.row) + "," + (p.col+1));

        return moves.toArray(new String[moves.size()]);
    }


    public String getPlayerActions(String d1, String d2, String card1, String card2, String board) throws Suspicion.BadActionException
    {
        this.board = new Board(board, pieces, gemLocations);
        String actions = "";

        // Random move for dice1
        if(d1.equals("?")) d1 = guestNames[r.nextInt(guestNames.length)];
        Piece piece = pieces.get(d1);
        String[] moves = getPossibleMoves(piece);
        int movei = r.nextInt(moves.length);
        actions += "move," + d1 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // Random move for dice2
        if(d2.equals("?")) d2 = guestNames[r.nextInt(guestNames.length)];
        piece = pieces.get(d2);
        moves = getPossibleMoves(piece);
        movei = r.nextInt(moves.length);
        actions += ":move," + d2 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // which card
        int i = r.nextInt(2);
        actions += ":play,card"+(i+1);

        String card = i==0?card1:card2;


        for(String cardAction: card.split(":")) // just go ahead and do them in this order
        {
            if(cardAction.startsWith("move")) 
            {
                String guest;
                guest = guestNames[r.nextInt(guestNames.length)];
                piece = pieces.get(guest);
                //moves = getPossibleMoves(piece);
                actions += ":move," + guest + "," + r.nextInt(3) + "," + r.nextInt(4);
            }
            else if(cardAction.startsWith("viewDeck")) 
            {
                actions += ":viewDeck";
            }
            else if(cardAction.startsWith("get")) 
            {
                String gemToGrab;
                int count;
                if(cardAction.equals("get,")) 
                {
                    // Grab a random gem
                    gemToGrab = this.board.rooms[me.row][me.col].availableGems[r.nextInt(this.board.rooms[me.row][me.col].availableGems.length)];
                    actions += ":get," + gemToGrab;
                }
                else 
                {
                    actions += ":" + cardAction;
                    gemToGrab=cardAction.trim().split(",")[1];
                }
                if(gemToGrab.equals("red")) gemCounts[Suspicion.RED]++;
                else if(gemToGrab.equals("green")) gemCounts[Suspicion.GREEN]++;
                else gemCounts[Suspicion.YELLOW]++;
            }
            else if(cardAction.startsWith("ask")) 
            {
                // Ask a random player
                actions += ":" + cardAction + otherPlayerNames[r.nextInt(otherPlayerNames.length)]; 
            }
        }
        return actions;
    }

    private int countGems(String gem)
    {
        if(gem.equals("red")) return gemCounts[Suspicion.RED];
        else if(gem.equals("green")) return gemCounts[Suspicion.GREEN];
        else return gemCounts[Suspicion.YELLOW];
    }

    private ArrayList<String> getGuestsInRoomWithGem(String board, String gemcolor)
    {
        Board b = new Board(board, pieces, gemLocations);
        int gem=-1;
        if(gemcolor.equals("yellow")) gem = Suspicion.YELLOW;
        else if(gemcolor.equals("green")) gem = Suspicion.GREEN;
        else if(gemcolor.equals("red")) gem = Suspicion.RED;
        ArrayList<String> possibleGuests = new ArrayList<String>();

        int y=0,x=0;
        for(String guests: board.trim().split(":"))
        {
            //only get people from rooms with the gem
            if(b.rooms[y][x].gems[gem] && guests.trim().length()>0)
            {
                for(String guest:guests.trim().split(","))
                {
                    possibleGuests.add(guest.trim());
                }
            }
            x++;
            y+=x/4;
            x%=4;
        }
        
        return possibleGuests;
    }

    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board, String actions)
    {
    }

    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board[], String actions)
    {
        if(player.equals(this.playerName)) return; // If player is me, return
        // Check for a get action and use the info to update player knowledge
        if(cardPlayed.split(":")[0].equals("get,") || cardPlayed.split(":")[1].equals("get,"))
        {
            int splitindex;
            String[] split = actions.split(":");
            String get;
            if(split[3].indexOf("get")>=0) splitindex=3;
            else splitindex=4;
            get=split[splitindex];
            String gem = get.split(",")[1];
            // board[splitIndex+1] will have the state of the board when the gem was taken
            if(board[splitindex]!=null) // This would indicate an error in the action
            {
                ArrayList<String> possibleGuests = getGuestsInRoomWithGem(board[splitindex],gem);
                players.get(player).adjustKnowledge(possibleGuests);
            }
        }
    }

    private boolean canSee(Piece p1, Piece p2) // returns whether or not these two pieces see each 
    {
        return (p1.row==p2.row || p1.col == p2.col);
    }

    
    public void answerAsk(String guest, String player, String board, boolean canSee)
    {
        Board b = new Board(board, pieces, gemLocations);
        ArrayList<String> possibleGuests = new ArrayList<String>();
        Piece p1 = pieces.get(guest);  // retrieve the guest 
        for(String k : pieces.keySet())
        {
            Piece p2 = pieces.get(k);
            if((canSee && canSee(p1,p2)) || (!canSee && !canSee(p1,p2))) possibleGuests.add(p2.name);
        }
        players.get(player).adjustKnowledge(possibleGuests);
    }

    public void answerViewDeck(String player)
    {
        for(String k:players.keySet())
        {
            players.get(k).adjustKnowledge(player);
        }
    }


/* Modify this method to do something more intelligent. */
    //k is the player we are guessing and p.possibleGuestNames has the possible options
    @Override
    public String reportGuesses()
    {    	
    	//assuming we have six players
    	int noOfPlayers = 6;
    	
    	//each item in the list is a string array containing the possible guesses of that player
    	List<String[]> playerGuessList = new ArrayList<String[]>();
    	
    	for(String k:players.keySet())
        {
            Player p = players.get(k);
            
            String[] playerGuesses = new String[p.possibleGuestNames.size() + 1];
            playerGuesses[0] = k;
            for(int i=1;i<p.possibleGuestNames.size() + 1 ; i++)
            {
            	playerGuesses[i] = p.possibleGuestNames.get(i-1);
            }
            //now we have a string array with guesses of this particular player
            //add it to the list
            playerGuessList.add(playerGuesses);
        }
    	
        //now in stead of random guessing, we do a table lookup
    	// for 6 players, the guess table of one player should have 5 lists
    	String startIndex = "";
    	String endIndex = "";
    	for(int i=0;i<noOfPlayers-1;i++)
    	{
    		startIndex += "1";
    		endIndex += "9";
    	}
    	
    	List<String> allCombinations = new ArrayList<String>();
        int totalCombinations = 0;
        
        String[] players = new String[noOfPlayers-1];
        while(!startIndex.equals(endIndex))
        {
        	String[] guessCombinations = new String[noOfPlayers-1];
        	String totalCombinationString = "";
        	
        	for(int i=0;i<noOfPlayers-1;i++)
        	{
        		int index = (int) (startIndex.charAt(i) - '0');	// the guess we are considering for this combination
        		
        		String[] temp = playerGuessList.get(i);
        		players[i] = temp[0];	// the first value is the name of the player we are guessing, store that
        		
        		if(index >= temp.length || index == 0)
        		{
        			guessCombinations[i] = "null";
        		}
        		else
        		{
        			guessCombinations[i] = temp[index];
        		}
        		
        		if(i == 0)
        		{
        			totalCombinationString += guessCombinations[i];
        		}
        		else
        		{
        			totalCombinationString += ":" + guessCombinations[i];
        		}
        		
        	}
        	
        	boolean check = true;
    		//check null
    		if(totalCombinationString.contains("null"))
    		{
    			check = false;
    		}
    		else	//guess if all combo are unique
    		{
    			for(int l = 0; l< guessCombinations.length;l++)
    			{
    				for(int m = l + 1; m< guessCombinations.length;m++)
    				{
    					if(guessCombinations[l].equals(guessCombinations[m]))
    					{
    						check = false;
    						break;
    					}
    				}
    			}
    				
    		}
    		if(check)
    		{
    			//add this into all combinations
    			allCombinations.add(totalCombinationString);
        		totalCombinations++;
    		}
    		else
    		{
    			// "not valid"
    		}
        	
    		//increment while loop condition
        	int sIndex = Integer.parseInt(startIndex);
        	sIndex++;
        	startIndex = Integer.toString(sIndex);
        }
        
        
        String[][] table = new String[totalCombinations][5];
        
        for(int i = 0; i < totalCombinations; i++)
        {
        	String[] line = allCombinations.get(i).split(":");
        	for(int j = 0; j < noOfPlayers-1; j++)
        	{
        		table[i][j] = line[j];
        	}
        }
        
        String[][] transposedTable = new String[5][totalCombinations];
        for(int i = 0; i < 5; i++)
        {
        	for(int j = 0; j < totalCombinations; j++)
        	{
        		transposedTable[i][j] = table[j][i];
        	}
        }
        
        //so now time to find out who is who
        String rval="";
        for(int i=0;i<5;i++)
        {
        	//get the player
        	String player = players[i];
        	rval += player;
        	
        	//get the character with max occurance
        	String[] guessForThisPlayer = transposedTable[i];
        	//first create a hashmap to calculate occurance
        	HashMap<String, Integer> hm = new HashMap<String, Integer>();
        	for (int j = 0; j < guessForThisPlayer.length; j++) {
                if ( hm.containsKey(guessForThisPlayer[j]) )
                {
                    Integer value = hm.get(guessForThisPlayer[j]);
                    hm.put(guessForThisPlayer[j], value + 1);
                }
                else
                {
                    hm.put(guessForThisPlayer[j], 1);
                }
            }
        	//now go through the hash map to find the value with max occurance
        	String bestGuess = "";
        	int occurance = 0;
        	for(String key: hm.keySet())
        	{
        		if((int)hm.get(key) > occurance)
        		{
        			bestGuess = key;
        			occurance = (int)hm.get(key);
        		}
        	}
        	
        	// we have the best guessed character
        	//store them
        	rval += "," + bestGuess + ":";
        }
    	
        return rval.substring(0,rval.length()-1);
    }

    public RBotProbabilistic(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames)
    {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        display = new TextDisplay(gemLocations);
        pieces = new HashMap<String, Piece>();
        ArrayList<String> possibleGuests = new ArrayList<String>();
        for(String name:guestNames)
        {
            pieces.put(name, new Piece(name));
            if(!name.equals(guestName)) possibleGuests.add(name);
        }
        me = pieces.get(guestName);

        players = new HashMap<String, Player>();
        for(String str: playerNames)
        {
            if(!str.equals(playerName)) players.put(str, new Player(str, possibleGuests.toArray(new String[possibleGuests.size()])));
        }

        otherPlayerNames = players.keySet().toArray(new String[players.size()]);
    }
}


