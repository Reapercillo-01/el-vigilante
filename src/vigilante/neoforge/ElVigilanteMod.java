package vigilante.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import vigilante.LanzadorVigilante;

/** Entrada del mod en NeoForge (solo cliente). El Vigilante, por Reapercillo. */
@Mod(value = "el_vigilante", dist = Dist.CLIENT)
public final class ElVigilanteMod {
    public ElVigilanteMod() {
        LanzadorVigilante.iniciar(FMLPaths.GAMEDIR.get());
    }
}
