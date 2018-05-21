/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.Serializable;

public class Message implements Serializable
{
    public enum MsgType { MSG_INVALID, MSG_DISCONNECT, MSG_ADD, MSG_MUL, MSG_POW, MSG_CONNECT, MSG_SHOW_ONLINE, MSG_PAIR, MSG_MSG};
    public MsgType mType;    
    
    Message() { mType = MsgType.MSG_INVALID; mN = 0; }
    
    public int mN;
    public int[] mNumbers;
    
    public int mA, mB;
    public String[] s;
}
