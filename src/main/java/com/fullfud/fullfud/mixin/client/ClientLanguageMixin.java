package com.fullfud.fullfud.mixin.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Gives the pre-reform Russian language a sane fallback.
 *
 * <p>{@code LanguageManager} always builds the code list as {@code [en_us, selected]}, and
 * {@code loadFrom} lets later codes overwrite earlier ones. Since {@code rpr} is registered by this
 * mod alone, selecting it would leave every vanilla key on English. Slipping {@code ru_ru} in front
 * of it makes the chain {@code en_us -> ru_ru -> rpr}: vanilla text stays modern Russian and only the
 * keys this mod actually ships get the pre-reform spelling.
 *
 * <p>{@code require = 0}, in line with the rest of {@code mixins.fullfud.json}: if the match ever
 * breaks, the language still works, just with an English base.
 */
@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {

    private static final String PRE_REFORM_CODE = "rpr";
    private static final String RUSSIAN_CODE = "ru_ru";

    @ModifyVariable(method = "loadFrom", at = @At("HEAD"), argsOnly = true, index = 1, require = 0)
    private static List<String> fullfud$insertRussianBase(final List<String> codes) {
        if (codes == null || !codes.contains(PRE_REFORM_CODE) || codes.contains(RUSSIAN_CODE)) {
            return codes;
        }
        final List<String> widened = new ArrayList<>(codes.size() + 1);
        for (final String code : codes) {
            if (PRE_REFORM_CODE.equals(code)) {
                widened.add(RUSSIAN_CODE);
            }
            widened.add(code);
        }
        return widened;
    }
}
