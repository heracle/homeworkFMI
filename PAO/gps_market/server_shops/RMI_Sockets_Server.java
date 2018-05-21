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
import java.util.Arrays;
import java.util.Comparator;

class piece_of_shop
{
    public int x, y;

    public piece_of_shop(int ax, int ay)
    {
        x = ax;
        y = ay;
    }

    public piece_of_shop()
    {
        x = -1;
        y = -1;
    }
}

class pair_distance
{
    public int index;
    public int sq_distance;
}


class shop_server implements shop_finder
{
    private static final int GRID_LIMIT = 100;
    private static final int MAX_NO_SHOPS = 50;

    private static piece_of_shop[] pool_shops;
    private static int size_available_pool;

    private static ArrayList<piece_of_shop> placed_shops;

    private static Random rand;

    private static Lock mutex;

    private static int check_in_grid(int x, int y)
    {
        if(x < 0 || x > GRID_LIMIT)
            return 0;
        if(y < 0 || y > GRID_LIMIT)
            return 0;
        return 1;
    }

    public static void initialization()
    {
        rand = new Random();
        pool_shops = new piece_of_shop[MAX_NO_SHOPS];

        mutex = new ReentrantLock(true);

        for(int i = 0; i < MAX_NO_SHOPS; i++)
        {
            pool_shops[i] = new piece_of_shop();
        }

        size_available_pool = MAX_NO_SHOPS;
        placed_shops = new ArrayList<piece_of_shop>();
    }

    public int make_one_shop(int x, int y)
    {
        if (size_available_pool == 0)
            return 0;
        if(check_in_grid(x, y) == 0)
            return 0;
        mutex.lock();
        size_available_pool --;
        piece_of_shop act = pool_shops[size_available_pool];

        act.x = x;

        act.y = y;

        placed_shops.add(act);

        mutex.unlock();

        return 1;
    }

    private int make_one_random_shop()
    {
        return make_one_shop(rand.nextInt(GRID_LIMIT + 1), rand.nextInt(GRID_LIMIT + 1));
    }

    public int make_shops(int k)
    {
        for(int i = 0; i < k; i++)
        {
            int resp = make_one_random_shop();
            if(resp == 0)
            {
                return i;
            }
        }
        return k;
    }

    public int remove_one_shop(int x, int y)
    {
        mutex.lock();
        for(int i = 0; i < placed_shops.size(); i++)
        {
            piece_of_shop act = placed_shops.get(i);

            if(act.x == x && act.y == y)
            {
                size_available_pool ++;
                pool_shops[size_available_pool - 1] = act;
                placed_shops.remove(i);
                mutex.unlock();
                return 1;
            }
        }
        mutex.unlock();
        return 0;
    }

    public shop_list give_closest_shops(int k, int x, int y)
    {
        if(placed_shops.size() == 0)
            return new shop_list(0);
        if(k > placed_shops.size())
            k = placed_shops.size();

        pair_distance[] v = new pair_distance[placed_shops.size()];

        for(int i = 0; i < placed_shops.size(); i++)
        {
            v[i] = new pair_distance();
        }

        mutex.lock();
        for(int i = 0; i < placed_shops.size(); i++)
        {
            piece_of_shop act = placed_shops.get(i);
            v[i].index = i;
            v[i].sq_distance = (x - act.x) * (x - act.x) + (y - act.y) * (y - act.y);
        }
        mutex.unlock();

        Arrays.sort(v, (a, b) -> Integer.signum(a.sq_distance - b.sq_distance)  );

        shop_list result = new shop_list(k);

        for(int i = 0; i < k; i++)
        {
            result.x_arr[i] = placed_shops.get(v[i].index).x;
            result.y_arr[i] = placed_shops.get(v[i].index).y;
        } 

        return result;
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
            shop_server serverOb = new shop_server();

            while(true)
            {
                String method = streamFromClient.readUTF();




                System.out.println("receive method: " + method);

                if(method.equals("make_shops"))
                {
                    int param = streamFromClient.readInt();
                    System.out.println("send message to class");
                    int result = serverOb.make_shops(param);
                    System.out.println("return message to class");
                    streamToClient.writeInt(result);
                    streamToClient.flush();
                }
                else if(method.equals("remove_one_shop"))
                {
                    int paramX = streamFromClient.readInt();
                    int paramY = streamFromClient.readInt();

                    int result = serverOb.remove_one_shop(paramX, paramY);
                    streamToClient.writeInt(result);
                    streamToClient.flush();
                }
                else if(method.equals("make_one_shop"))
                {
                    int paramX = streamFromClient.readInt();
                    int paramY = streamFromClient.readInt();

                    int result = serverOb.make_one_shop(paramX, paramY);
                    streamToClient.writeInt(result);
                    streamToClient.flush();
                }
                else if(method.equals("give_closest_shops"))
                {
                    int paramK = streamFromClient.readInt();

                    int paramX = streamFromClient.readInt();
                    int paramY = streamFromClient.readInt();


                    shop_list result = serverOb.give_closest_shops(paramK, paramX, paramY);

                    streamToClient.writeInt(result.size);
                    streamToClient.flush();

                    for(int i = 0; i < result.size; i++)
                    {
                        streamToClient.writeInt(result.x_arr[i]);
                        streamToClient.flush();
                        streamToClient.writeInt(result.y_arr[i]);
                        streamToClient.flush();
                    }
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
    private static final int PORT = 1101;

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
            shop_server.initialization();
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
