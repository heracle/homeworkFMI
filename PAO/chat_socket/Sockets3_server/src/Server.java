import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
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

class pair_Name_IP
{
    String Name, Ip;

    pair_Name_IP(String name, String ip)
    {
        this.Name = name;
        this.Ip = ip;
    }
}


class ClientHandler extends Thread
{
    private Socket              mClientSocket;
    
    public ObjectInputStream    mStreamFromClient;
    public ObjectOutputStream   mStreamToClient;        
    public String               mIPAddress;
    public String               mName;

    public ClientHandler        pair;

    public void get_new_message(String Sender, String message)
    {
        try{
            TaskResult res = new TaskResult();

            res.mResult = "__&#((@ " + Sender + message;

            mStreamToClient.writeObject(res);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }


    public ClientHandler(Socket socket)
    {
        mClientSocket   = socket;
        pair = null;
        mName = "";
        try
        {
            mStreamFromClient   = new ObjectInputStream(mClientSocket.getInputStream());
            mStreamToClient     = new ObjectOutputStream(mClientSocket.getOutputStream()); 
            mStreamToClient.flush(); // Good to call this 
            
            mIPAddress          = socket.getInetAddress().getHostAddress();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }                
    }
        
    public void run()
    {
        try 
        {
            boolean stillConnected = true;
            while(stillConnected)
            {                
                Message msg = (Message) mStreamFromClient.readObject();
        
                int result = 0;
                TaskResult res = new TaskResult();

                switch(msg.mType)
                {
                    case MSG_DISCONNECT:
                    {
                        System.out.println("client " + mName + " got disconnected..");                        
                        stillConnected = false;
                        mClientSocket.close();
                        
                        Server.removeClient(this);
                    }
                    break;
                    case MSG_ADD:
                    {                                               
                        System.out.println("client " + mName + " requested add, sending result back..");
                        
                        result = 0;
                        for (int i = 0; i < msg.mN; i++)
                            result += msg.mNumbers[i];

                        res.mResult = "" + result;                                                
                    }
                    break;
                    case MSG_MUL:
                    {
                        System.out.println("client " + mName + " requested mul, sending result back..");
                        
                        result = 1;
                        for (int i = 0; i < msg.mN; i++)
                            result *= msg.mNumbers[i];


                        res.mResult = "" + result;
                    }
                    break;
                    case MSG_POW:
                    {
                        System.out.println("client " + mName + " requested pow, sending result back..");
                                
                        result = (int)Math.pow((double)msg.mA, (double)msg.mB);

                        res.mResult = "" + result;
                    }
                    break;
                    case MSG_CONNECT:
                    {
                        this.mName = msg.s[0];
                        res.mResult = mIPAddress;

                        int answer_add = Server.add_data_base(mName, mIPAddress);

                        if(answer_add == 0)//full database
                        {
                            System.out.println("user \"" + msg.s[0] + "\" tried to enter into chat, but there are no any free places");
                            msg.mType = Message.MsgType.MSG_DISCONNECT;
                            res.mResult = "Server is full";
                        }
                        else if(answer_add == -1) //conflict with names
                        {
                            System.out.println("conflict with names \"" + msg.s[0] + "\"");
                            res.mResult = "invalid name";
                        }
                        else
                            System.out.println("new user \"" + msg.s[0] + "\" entered in chat with IP " + mIPAddress);

                    }
                    break;
                    case MSG_SHOW_ONLINE:
                    {
                        System.out.println("client " + mName + " requested to see the list of online users");
                        res.mResult = Server.show_active_users();
                    }
                    break;
                    case MSG_PAIR:
                    {
                        System.out.println("client " + mName + " requested to pair with " + msg.s[0]);
                        pair = Server.find_client_by_name(msg.s[0]);
                        if(pair != null)
                            res.mResult = "Paired";
                        else
                            res.mResult = "fail to pairing";
                    }
                    break;
                    case MSG_MSG:
                    {
                        System.out.println("client \"" + mName + "\" send to \"" + msg.s[0] + "\" a message\n" + msg.s[1]);

                        if(msg.s[0].compareTo("all") == 0) //global message
                        {
                            Server.send_to_all(mName, msg.s[1]);
                            res.mResult = "Sent to everyone";
                        }
                        else //personal message
                        {
                            ClientHandler receiver = Server.find_client_by_name(msg.s[0]);
                            if(receiver != null)
                            {
                                res.mResult = "Sent";
                                receiver.get_new_message(mName, msg.s[1]);
                            }
                            else
                                res.mResult = "can not find the receiver"; 
                        }

                        
                    }
                    break;
                }             


                
                if (msg.mType != Message.MsgType.MSG_DISCONNECT)
                    mStreamToClient.writeObject(res);
            }            
            
        }
        catch(Exception e) { 
            System.out.println(e.getMessage()); 
            System.out.println("It's not ok to quit chat with CTRL+C  user: " + mName);
                 
            Server.removeClient(this);
        }
    }    
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
                System.out.println("Num clients connected = " + Server.mClientsList.size());
                for (ClientHandler ch : Server.mClientsList)
                {
                    System.out.print(ch.mIPAddress + " ");
                }
            }
        }
    }
}

public class Server 
{  
    public static ServerSocket              mServerSocket;
    public static ArrayList<ClientHandler>  mClientsList;



    static pair_Name_IP[] list;
    static int no_connected;
    static int MAX_CONNECTIONS;

    static Lock mutex;/*
        used when add-in new connection into the list
        or removing...
    */

    static void send_to_all(String sender, String text)
    {
        mutex.lock();

        for(int i = 0; i < mClientsList.size(); i++)
        {
            ClientHandler receiver = mClientsList.get(i);
         	
         	if(receiver.mName == "")
         		continue;

            if(sender.compareTo(receiver.mName) != 0)
            {
                receiver.get_new_message(sender, text);
            }
        }

        mutex.unlock();
    }

    static ClientHandler find_client_by_name(String name)
    {
        mutex.lock();

        for(int i = 0; i < mClientsList.size(); i++)
        {
            ClientHandler act = mClientsList.get(i);

            if( name.compareTo(act.mName) == 0)
            {
                mutex.unlock();
                return act;
            }
        }


        mutex.unlock();
        return null;

    }

    static String show_active_users()
    {
        String result = "";
        mutex.lock();

        for(int i = 0; i < mClientsList.size(); i++)
        {
            ClientHandler act = mClientsList.get(i);

            result = result + "name: " + act.mName + "   ip:" + act.mIPAddress + "\n";
        }

        
        mutex.unlock();

        return result;
    }

    static void Create(int server_size)
    {
        MAX_CONNECTIONS = server_size;
        no_connected = 0;
        list = new pair_Name_IP[MAX_CONNECTIONS + 1];
        mutex = new ReentrantLock(true);
    }

    static int add_data_base(String name, String ip)
    {
        if(no_connected == MAX_CONNECTIONS)
            return 0;
        mutex.lock();

        if(name.compareTo("all") == 0 || name.compareTo("server") == 0)
        {
            System.out.println("Un bulangiu");
            return -1;
        }

        for(int i = 1; i <= no_connected; i++)
        {
            if(name.compareTo(list[i].Name) == 0 )
                return -1;
        }

        no_connected ++;
        list[no_connected] = new pair_Name_IP(name, ip);

        mutex.unlock();

    	send_to_all("server", " \"" + name + "\" has connected");

        return 1;
    } 
    static void remove_data_base(String name)
    {
        mutex.lock();

        for(int i = 1; i <= no_connected; i++)
        {
            if( list[i].Name.compareTo(name) == 0 )
            {
                list[i] = list[no_connected];
                no_connected --;
                break;
            }
        }
        mutex.unlock();


    	send_to_all("server", " \"" + name + "\" has disconnected");
    }
    public static void removeClient(ClientHandler ch) 
    {
        remove_data_base(ch.mName);
        mutex.lock();
        mClientsList.remove(ch);
        mutex.unlock();
    }
    
    public static void addClient(ClientHandler ch)
    {
        mutex.lock();
        mClientsList.add(ch);
        mutex.unlock();

    }


            
    public static void main(String[] args) 
    {
        final int SERVER_SIZE = 100; // final is constant
        Server.Create(SERVER_SIZE); // 100 placed for this chat

        try 
        {
            mServerSocket   = new ServerSocket(9090);
            mClientsList    = new ArrayList<ClientHandler>();
            
            ServerConsole sc = new ServerConsole();
            sc.start();
            
            while(true)
            {
                Socket socket = mServerSocket.accept();
                ClientHandler ch = new ClientHandler(socket);
                addClient(ch);
                ch.start();
                
            }           
        }
        catch(Exception e)
        {
            System.out.print(e.getMessage());      
        }
    }    
    
   
}
