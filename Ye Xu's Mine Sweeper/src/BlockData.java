import java.io.Serializable;
import java.util.ArrayList;

//used specifically to save mine block data
public class BlockData implements Serializable {
    int rowSave;
    int colSave;
    int mineAroundSave;
    boolean isMineSave;
    boolean isFlagSave;
    boolean isRevealedSave;

    void update(int inRow, int inCol, int inMineAround, boolean isMine,
                boolean isFlag, boolean isRevealed){
        rowSave = inRow;
        colSave = inCol;
        mineAroundSave = inMineAround;
        isMineSave = isMine;
        isFlagSave = isFlag;
        isRevealedSave = isRevealed;
    }
}
