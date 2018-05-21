/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.Console;
import java.util.Scanner;

import javax.swing.JOptionPane;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;


class ClientReceive extends Thread
{
    private static TaskResult last_update_received = null;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    static Lock mutex;

    public static TaskResult get_answer()
    {
    	
    	mutex.lock();
		try
    	{
	        while(ClientReceive.last_update_received == null)
	        {
	            System.out.println(" wait...");
	            mutex.unlock();
	            Thread.sleep(100);
	            mutex.lock();
	        }
	    }
        catch (Exception e)
    	{
    	    System.out.println("exception from get_answer()");
        	System.out.println(e.getMessage());
    	}
    	

        TaskResult result = new TaskResult();
        result.mResult = last_update_received.mResult;
        last_update_received = null;
        mutex.unlock();
        return result;

	    

    }

    public void run()
    {
        try{
            ObjectInputStream streamFromServer = new ObjectInputStream(Client.clientSocket.getInputStream());

            while(true)
            {      
            	mutex.lock();
                while(last_update_received != null)
                {
                    mutex.unlock();
                    Thread.sleep(100);
                    mutex.lock();
                }
                
                TaskResult result = (TaskResult) streamFromServer.readObject();

                String[] tokens = result.mResult.split(" ");


                if(tokens[0].compareTo("__&#((@") == 0)
                {
                    String text = "";

                    for(int i = 2; i < tokens.length; i++)
                        text = text + tokens[i] + " ";

                    System.out.println(ANSI_BLUE + tokens[1] + ANSI_RESET + ANSI_CYAN + " -->  " + ANSI_RESET + ANSI_GREEN + text + ANSI_RESET);
                }
                else
                {
                    last_update_received = result;                    
                }

                mutex.unlock();

            }
        }
        catch (Exception e)
        {
            System.out.println("exception from run()");
            System.out.println(e.getMessage());
        }

    }
}


public class Client 
{
    static String Guide_chat = " \"active_? \" for printing active users \n" +
                               " \"pair_? $name\" for pairing with another active user\n" +
                               " \"my_pair_?\" for checking who is the actual pair\n" +
                               " \"unpair_?\" for unpair\n" +
                               " \"me_?\" for receiveing your name\n" +
                               " \"send_? all text\" for sending a message with \"text\" to everybody\n" +
                               " \"send_? $name text\" for sending a message with \"text\" to $name\n"  +
                               " \"text\" If already paired, \"text\" will be send to the pair\n" +
                               " \"disconnect_?\" for disconnect" ;


    static String paired = "";
    static String try_pair_with = "";
    static String my_name = "";

    public static Message parseMessage(String msgText)
    {
        final String[] tokens = msgText.split(" ");
        Message msg = new Message();
        
        if (tokens[0].compareTo("disconnect_?") == 0) 
        { 
            msg.mType = Message.MsgType.MSG_DISCONNECT; 
        }
        else if (tokens[0].compareTo("connect_?#$%^$##@") == 0)
        {
            msg.mType = Message.MsgType.MSG_CONNECT;

            msg.s = new String[1];

            msg.s[0] = tokens[1];
        }
        else if (tokens[0].compareTo("active_?") == 0)
        {
            msg.mType = Message.MsgType.MSG_SHOW_ONLINE;
        }
        else if (tokens[0].compareTo("pair_?") == 0)
        {
            msg.mType = Message.MsgType.MSG_PAIR;
            msg.s = new String[1];
            msg.s[0] = tokens[1];
            try_pair_with = tokens[1];
        }
        else if (tokens[0].compareTo("send_?") == 0)
        {
            msg.mType = Message.MsgType.MSG_MSG;
            msg.s = new String[2];
            msg.s[0] = tokens[1];
            msg.s[1] = "";
            for(int i = 2; i < tokens.length; i++)
            {
                msg.s[1] = msg.s[1] + " " + tokens[i];
            }
        }
        else if(paired.compareTo("") != 0)
        {
            msg.mType = Message.MsgType.MSG_MSG;
            msg.s = new String[2];
            msg.s[0] = paired;
            msg.s[1] = "";
            for(int i = 0; i < tokens.length; i++)
            {
                msg.s[1] = msg.s[1] + " " + tokens[i];
            }
        }
        return msg;
    }
    
    static String serverAddress;
    static Socket clientSocket;

    public static void main(String[] args) 
    {

        ClientReceive.mutex = new ReentrantLock(true);
        try
        {
            serverAddress = "localhost"; //JOptionPane.showInputDialog("enter IP " + "(running on port 9090");
            clientSocket = new Socket(serverAddress, 9090);
            
            ObjectOutputStream streamToServer = new ObjectOutputStream(clientSocket.getOutputStream());


            Scanner scan = new Scanner(System.in);


            int check_availability = 0;


            ClientReceive Client_waiter = new ClientReceive(); 
            Client_waiter.start();


            while(check_availability == 0)
            {
                System.out.println("Please enter your name:");   

                String name = scan.nextLine();

                String msgText = "connect_?#$%^$##@ " + name;


                Message msg = parseMessage(msgText); 

                streamToServer.writeObject(msg);

                TaskResult result = ClientReceive.get_answer();

                if(result.mResult.compareTo("invalid name") == 0)
                {
                    System.out.println("Try another name, this one is already taken");
                }
                else if(result.mResult.compareTo("server_full") == 0)
                {
                    System.out.println("Server is full");
                    return;
                }
                else 
                {    
                    System.out.println("It's me, " + name + ", I am connected with ip " + result.mResult);
                    check_availability = 1;
                    my_name = name;
                }
            }

            
            System.out.println(Guide_chat);  

            while(true)
            {
                String msgText = scan.nextLine();

                if(msgText.compareTo("my_pair_?") == 0)
                {
                    System.out.println("I am paired with \"" + paired + "\"");
                    continue;
                }
                if(msgText.compareTo("me_?") == 0)
                {
                    System.out.println("I am \"" + my_name + "\"");
                    continue;
                }
                if(msgText.compareTo("unpair_?") == 0)
                {
                    paired = "";
                    System.out.println("unpaired");
                }

                Message msg = parseMessage(msgText);
                
                if (msg.mType == Message.MsgType.MSG_INVALID)
                {
                    System.out.println("Incorrect message format, try again");
                    System.out.println(Guide_chat);  
                }
                else
                {                   
                    streamToServer.writeObject(msg);
                    if (msg.mType == Message.MsgType.MSG_DISCONNECT)    // Normally we should wait for disconnect confirm for server but this is fine too.
                        break;

               		TaskResult result = ClientReceive.get_answer();


                    if(msg.mType == Message.MsgType.MSG_PAIR)
                    {
                        if(result.mResult.compareTo("Paired") == 0)
                        {
                            paired = try_pair_with;
                        }
                    }

                    System.out.println("Result is:\n" + result.mResult);
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("exception from main()");
            System.out.println(e.getMessage());
        }
    }    
}
