import java.io.Serializable;


//complete data save package, used to store in DB
public class DataSave implements Serializable {
    String player = "Unknown";
    int remainFlag;
    int remainMine;
    long remainTime;
    boolean isComplete = false;
    BlockData[][] AllBlockData;

    @Override
    public String toString() {
        return player +"'s save, Completion: " + isComplete +
                ", remain time: " + remainTime/1000 + " seconds.";
    }
    
    public void update(String in_player, int flag_number, int remaining_mine,
                       long inTime, boolean inComplete, BlockData[][] field){
        player = in_player;
        remainFlag = flag_number;
        remainMine = remaining_mine;
        remainTime = inTime;
        isComplete = inComplete;
        AllBlockData = field;
    }
}
