import java.lang.invoke.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import jdk.incubator.foreign.*;

/**
 %JDK16%\bin\java -Dforeign.restricted=permit --add-modules jdk.incubator.foreign SetWallpaper.java A.JPG
 */
public class ModularWallpaper {
    static final int SPI_SETDESKWALLPAPER  = 0x0014;
    static final int SPIF_UPDATEINIFILE    = 0x01;
    static final int SPIF_SENDCHANGE       = 0x02;

    /**
     %JAVA_HOME%\bin\java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign SetWallpaper.java A.JPG
     */
    public static void main(String[] args) throws Throwable {
        System.loadLibrary("user32");
        // BOOL SystemParametersInfoA(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni);
        MemoryAddress symbol = SymbolLookup.loaderLookup().lookup("SystemParametersInfoW").get();
        MethodHandle SystemParametersInfoW = CLinker.getInstance().downcallHandle(symbol
                , MethodType.methodType(int.class,     int.class,      int.class,      MemoryAddress.class, int.class)
                , FunctionDescriptor.of(CLinker.C_LONG,CLinker.C_LONG, CLinker.C_LONG, CLinker.C_POINTER,   CLinker.C_LONG));

        Path path = Path.of(args[0]).toRealPath();

        try(ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            // toCString as WIDE string
            Addressable wide = allocator.allocateArray(CLinker.C_CHAR, (path+"\0").getBytes(StandardCharsets.UTF_16LE));
            int status = (int)SystemParametersInfoW.invokeExact(SPI_SETDESKWALLPAPER, 0, wide.address(), SPIF_UPDATEINIFILE | SPIF_SENDCHANGE);
            System.out.println("Changed wallpaper to "+path+" rc="+status+(status == 0 ? " *** ERROR ***": " OK"));
        }
    }
}