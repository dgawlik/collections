package serialization;


import java.io.Serializable;

public class Pair implements Serializable {
  Integer first;
  Integer second;

  public Pair(Integer first, Integer second){
    this.first = first;
    this.second = second;
  }

  public byte[] toBytes(){
    byte[] arr = new byte[8];
    arr[0] = (byte)(this.second >> 24);
    arr[1] = (byte)((this.second >> 16) & 255);
    arr[2] = (byte)((this.second >> 8) & 255);
    arr[3] = (byte)((this.second ) & 255);
    arr[4] = (byte)(this.first >> 24);
    arr[5] = (byte)((this.first >> 16) & 255);
    arr[6] = (byte)((this.first >> 8) & 255);
    arr[7] = (byte)((this.first ) & 255);
    return arr;
  }
}
