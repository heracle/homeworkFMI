package rmi_sockets_client;

class shop_list
{
	public int[] x_arr;
	public int[] y_arr;
	public int size;

	shop_list(int rec_size)
	{
		size = rec_size;
		x_arr = new int[size];
		y_arr = new int[size];
	}
}


public interface shop_finder
{
	public int make_shops(int k);
	public int remove_one_shop(int x, int y);
	public int make_one_shop(int x, int y);
	public shop_list give_closest_shops(int k, int x, int y);
	public void terminate();  
}


