/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi_sockets_client;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.*;
import java.io.*;




class shop_client implements shop_finder
{

    Socket m_socket;
    DataOutputStream m_output;
    DataInputStream m_input;
    
    static final int PORT = 1101;
    static final String IP = "localhost";
    
    shop_client()
    {
    	try
    	{
	        m_socket = new Socket(IP, PORT);
	        m_output = new DataOutputStream(m_socket.getOutputStream());
	        m_input = new DataInputStream(m_socket.getInputStream());
    	}
    	catch(Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("exception in gps_client constructor");
        }
    }
    





    public shop_list give_closest_shops(int k, int x, int y)
    {
        try
        {
            m_output.writeUTF("give_closest_shops");
            m_output.flush();

            m_output.writeInt(k);
            m_output.flush();

            m_output.writeInt(x);
            m_output.flush();

            m_output.writeInt(y);
            m_output.flush();

            int actual_size = m_input.readInt();

            shop_list result = new shop_list(actual_size);

            for(int i = 0; i < result.size; i++)
            {
                result.x_arr[i] = m_input.readInt();
                result.y_arr[i] = m_input.readInt();
            }   

            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public int make_one_shop(int x, int y)
    {
        try
        {
            m_output.writeUTF("make_one_shop");
            m_output.flush();

            m_output.writeInt(x);
            m_output.flush();

            m_output.writeInt(y);
            m_output.flush();

            int result = m_input.readInt();
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public int remove_one_shop(int x, int y)
    {
        try
        {
            m_output.writeUTF("remove_one_shop");
            m_output.flush();

            m_output.writeInt(x);
            m_output.flush();

            m_output.writeInt(y);
            m_output.flush();

            int result = m_input.readInt();
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return -1;
        }
    }
    
    public int make_shops(int k)
    {
        try
        {
            m_output.writeUTF("make_shops");
            m_output.flush();

            m_output.writeInt(k);
            m_output.flush();

            int result = m_input.readInt();
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return -1;
        }
    }


    public void terminate()
    {
        try{
            m_output.writeUTF("terminate");
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
}



class gps_client implements gps
{

    Socket m_socket;
    DataOutputStream m_output;
    DataInputStream m_input;
    
    static final int PORT = 1100;
    static final String IP = "localhost";
    
    gps_client()
    {
        try
        {
            m_socket = new Socket(IP, PORT);
            m_output = new DataOutputStream(m_socket.getOutputStream());
            m_input = new DataInputStream(m_socket.getInputStream());
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("exception in gps_client constructor");
        }
    }
    

    public int move_me_random()
    {
        try
        {
            m_output.writeUTF("move_me_random");
            m_output.flush();
            int result = m_input.readInt();
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return -1;
        }
        
    }
    public int move_me_fixed(coordinates position)
    {
        try
        {
            m_output.writeUTF("move_me_fixed");
            m_output.flush();

            m_output.writeInt(position.x);
            m_output.flush();

            m_output.writeInt(position.y);
            m_output.flush();

            int result = m_input.readInt();
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return -1;
        }
        
    }

    public coordinates give_my_location()
    {
        try
        {
            System.out.println("in function to send request");
            

            m_output.writeUTF("give_my_location");
            m_output.flush();


            System.out.println("request sent");

            int X = m_input.readInt();
            int Y = m_input.readInt();

            coordinates result = new coordinates(X, Y);
            return result;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return null;
        }
        
    }

    public void terminate()
    {
        try{
            m_output.writeUTF("terminate");
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    
}



public class RMI_Sockets_Client 
{
	public static String guide = 		"public int move_me_random() \n" +
										"public int move_me_fixed(coordinates new_coord); \n" +
										"public coordinates give_my_location(); \n" +
										"public void terminate(); \n\n" +
                                        "public int make_shops(int k); \n" +
                                        "public int remove_one_shop(int x, int y); \n" +
                                        "public int make_one_shop(int x, int y); \n" +
                                        "public shop_list give_closest_shops(int k, int x, int y);\n" +
                                        "public void terminate();\n" ;

    

    public static void main(String[] args) 
    {
        
        try{
            gps_client obc = new gps_client();
            shop_client obs = new shop_client();
            Scanner sc = new Scanner(System.in);


    // public int move_me_random();
    // public int move_me_fixed(coordinates new_coord);
    // public coordinates give_my_location();
    // public void terminate();  

            System.out.println(guide);
            while(true)
            {
            	String request = sc.next();

            	if (request.equals("move_me_random"))
            	{
            		if( obc.move_me_random()  == 1 )
            		{
            			System.out.println("moved random");
            		}
            		else
            		{
            			System.out.println("failed to move random");
            		}
            	}
            	else if(request.equals("move_me_fixed"))
            	{
            		coordinates position = new coordinates(sc.nextInt(), sc.nextInt());

            		if(obc.move_me_fixed(position) == 1)
            		{
            			System.out.println("moved fixed");
            		}
            		else
            		{
            			System.out.println("failed to move fixed");
            		}
            	}
            	else if(request.equals("give_my_location"))
            	{
            		System.out.println("read the request");
            		coordinates position = obc.give_my_location();

            		System.out.println("I am at " + position.x + " " + position.y);
            	}
                else if(request.equals("make_shops"))
                {
                    int k = sc.nextInt();
                    int result = obs.make_shops(k);
                    System.out.println(result + " shops created");
                }
                else if(request.equals("remove_one_shop"))
                {
                    int x = sc.nextInt();
                    int y = sc.nextInt();
                    int result = obs.remove_one_shop(x, y);
                    if(result == 1)
                    {
                        System.out.println("shop " + x + " " + y + " deleted");
                    }
                    else
                    {
                        System.out.println("fail to delete this shop");
                    }
                }
                else if(request.equals("make_one_shop"))
                {
                    int x = sc.nextInt();
                    int y = sc.nextInt();
                    int result = obs.make_one_shop(x, y);
                    if(result == 1)
                    {
                        System.out.println("shop " + x + " " + y + " created");
                    }
                    else
                    {
                        System.out.println("fail to create shop");
                    }
                }
                else if(request.equals("give_closest_shops"))
                {
                    int k = sc.nextInt();
                    int x = sc.nextInt();
                    int y = sc.nextInt();
                    shop_list result = obs.give_closest_shops(k, x, y);

                    for(int i = 0; i < result.size; i++)
                    {
                        System.out.println("no: " + i + " x: " + result.x_arr[i] + " y: " + result.y_arr[i]);
                    }

                }

            	else if (request.equals("terminate"))
                {
                    obc.terminate();
                    obs.terminate();
                    break;
                }
                else
                {
                	System.out.println(guide);

                    System.out.println("Invalid method, please try another one after this line");
                }
            }

        }
        catch(Exception e) { System.out.println(e.getMessage());}
    }
    
}
