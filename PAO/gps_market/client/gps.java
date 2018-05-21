/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi_sockets_client;

class coordinates
{
	public int x, y;

	coordinates(int t1, int t2)
	{
		x = t1;
		y = t2;
	}
}

public interface gps 
{
    public int move_me_random();
    public int move_me_fixed(coordinates new_coord);
    public coordinates give_my_location();
    public void terminate();  
}
