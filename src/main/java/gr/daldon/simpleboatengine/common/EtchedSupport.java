package gr.daldon.simpleboatengine.common;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.fml.ModList;

/** Optional reflection bridge: Etched is NOT a required dependency. */
public final class EtchedSupport {
    private EtchedSupport(){}
    private static Class<?> api(){
        if(!ModList.get().isLoaded("etched"))return null;
        try{return Class.forName("gg.moonflower.etched.api.record.PlayableRecord");}
        catch(ReflectiveOperationException ignored){return null;}
    }
    public static boolean isPlayable(ItemStack item){
        if(item.isEmpty())return false;
        if(item.getItem() instanceof RecordItem)return true;
        Class<?> type=api();
        if(type==null)return false;
        try{
            Method check=type.getMethod("isPlayableRecord",ItemStack.class);
            return Boolean.TRUE.equals(check.invoke(null,item));
        }catch(ReflectiveOperationException | LinkageError error){return false;}
    }
    public static boolean play(Entity boat,ItemStack item){
        Class<?> type=api();
        if(type==null)return false;
        try{
            type.getMethod("playEntityRecord",Entity.class,ItemStack.class,boolean.class)
                .invoke(null,boat,item,true);
            return true;
        }catch(ReflectiveOperationException | LinkageError error){return false;}
    }
    public static void stop(Entity boat){
        Class<?> type=api();
        if(type==null)return;
        try{
            type.getMethod("stopEntityRecord",Entity.class).invoke(null,boat);
        }catch(ReflectiveOperationException | LinkageError ignored){}
    }
}
