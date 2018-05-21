/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi_sockets_server;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.Random;

class gps_server implements gps
{
    private static int LIMIT = 100;
    private coordinates my_coord;
    private Random rand;

    private static int check_in_grid(coordinates coord)
    {
        if(coord.x < 0 || coord.x > LIMIT)
            return 0;
        if(coord.y < 0 || coord.y > LIMIT)
            return 0;
        return 1;
    }

    public gps_server()
    {
        rand = new Random();
        my_coord = new coordinates(rand.nextInt(LIMIT + 1), rand.nextInt(LIMIT + 1));
    }

    public int move_me_random()
    {
        my_coord.x = rand.nextInt(LIMIT + 1);
        my_coord.y = rand.nextInt(LIMIT + 1); 
        return 1;
    }
    public int move_me_fixed(coordinates new_coord)
    {
        if(check_in_grid(new_coord) == 0)
            return 0;
        my_coord = new_coord;
        return 1;
    }

    public coordinates give_my_location()
    {
        return my_coord;
    }

    public void terminate() {}  
}



class ServerConsole extends Thread
{
    public void run()
    {
        Scanner scan = new Scanner(System.in);
        
        while(true)
        {
            String commandStr = scan.nextLine();
            if (commandStr.compareTo("showClients") == 0)
            {
                System.out.println("Num clients connected = " + RMI_Sockets_Server.get_no_cliets() );
            }
        }
    }
}

class ClientHandler extends Thread
{
    private Socket              clientSocket;
    private DataInputStream   streamFromClient;
    private DataOutputStream  streamToClient;  
    public String               IPAddress;



    public ClientHandler(Socket socket)
    {
        clientSocket   = socket;
        try
        {
            streamToClient     = new DataOutputStream(clientSocket.getOutputStream()); 
            streamToClient.flush(); // Good to call this
            streamFromClient   = new DataInputStream(clientSocket.getInputStream());
            IPAddress          = clientSocket.getInetAddress().getHostAddress();
            // System.out.println("finished init client handler");
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("exception in ClientHandler contructor");
        }                
    }
    
    public void run()
    {
        try 
        {
            gps_server serverOb = new gps_server();

            while(true)
            {
                String method = streamFromClient.readUTF();


                System.out.println("receive method: " + method);

                if (method.equals("move_me_random"))
                {
                    int result = serverOb.move_me_random();
                    System.out.println(result + " for move_me_random");
                    streamToClient.writeInt(result);
                    streamToClient.flush();
                }
                else if (method.equals("move_me_fixed"))
                {
                    int paramA = streamFromClient.readInt();
                    int paramB = streamFromClient.readInt();
                    coordinates position = new coordinates(paramA, paramB);
                    int result = serverOb.move_me_fixed(position);
                    System.out.println(result + " for move_me_fixed");
                    streamToClient.writeInt(result);
                    streamToClient.flush();
                }
                else if (method.equals("give_my_location"))
                {
                    coordinates act = serverOb.give_my_location();
                    System.out.println(act.x + " " + act.y + " for give_my_location");
                    streamToClient.writeInt(act.x);
                    streamToClient.flush();
                    streamToClient.writeInt(act.y);
                    streamToClient.flush();
                }
                else if (method.equals("terminate"))
                {
                    break;
                }
                else
                {
                    System.out.println("invalid request, try another method after this line");
                }
            }        
        }
        catch(Exception e) { 
            System.out.println(e.getMessage()); 
            System.out.println("exception in ClientHandler run()");        
            RMI_Sockets_Server.removeClient(this);
        }
    }   
}



public class RMI_Sockets_Server 
{
    private static final int PORT = 1100;
    private static final int MAX_PLACES = 20;

    private static ServerSocket serverSocket;
    private static ArrayList<ClientHandler>  ClientsList;
    private static Lock mutex_clientsList;


    private static void add_client(ClientHandler ch)
    {
        mutex_clientsList.lock();

        ClientsList.add(ch);

        mutex_clientsList.unlock();
    }

    public static int get_no_cliets()
    {
        return ClientsList.size();
    }


    public static void removeClient(ClientHandler ch)
    {
        mutex_clientsList.lock();
        ClientsList.remove(ch);
        mutex_clientsList.unlock();
    }

    private static void initialization()
    {
        try{
            mutex_clientsList = new ReentrantLock(true);
            serverSocket = new ServerSocket(PORT);
            ClientsList = new ArrayList<ClientHandler>();
            ServerConsole sc = new ServerConsole();
            sc.start();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("exception in initialization of Server");
        }
    }

    public static void main(String[] args) 
    {      
        try{
            initialization();
            while(true)
            {
                System.out.println("waiting for users");
                Socket socket = serverSocket.accept();
                ClientHandler ch = new ClientHandler(socket);

                System.out.println("new user came");

                add_client(ch);

                ch.start();
            }   
        }
        catch(Exception e) { System.out.println(e.getMessage()); }         
        
    }
    
}
