package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.shadowrun.Focus;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IFocusController;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-1030: Foci may be restricted to certain magic types
 */
public class FocusRequirementTest {

	private final static UUID QI_POWER  = UUID.fromString("37026c81-d5a0-44fe-8fa9-9263acb6059f");
	private final static UUID WEAPON    = UUID.fromString("de69a2e0-1d0a-4a14-8c88-08f5b3377dad");

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	private static KarmaCharacterGenerator create(String magicType) {
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		charGen.setModel(new Shadowrun6Character(), null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, magicType));
		charGen.runProcessors();
		return charGen;
	}

	//-------------------------------------------------------------------
	private static Possible check(KarmaCharacterGenerator charGen, String focusID, Decision...decisions) {
		Focus focus = Shadowrun6Core.getItem(Focus.class, focusID);
		assertNotNull(focusID, focus);
		IFocusController ctrl = charGen.getFocusController();
		return ctrl.canBeSelected(focus, decisions);
	}

	//-------------------------------------------------------------------
	private static void assertRequirementsMet(Possible poss) {
		assertTrue("Requirements should be met, but "+poss, poss.getUnfulfilledRequirements().isEmpty());
	}

	//-------------------------------------------------------------------
	private static void assertRequirementsNotMet(Possible poss) {
		assertFalse(poss.get());
		assertFalse("Requirements should not be met, but "+poss, poss.getUnfulfilledRequirements().isEmpty());
	}

	//-------------------------------------------------------------------
	@Test
	public void testQiFocusOnlyForAdepts() {
		Decision power = new Decision(QI_POWER, "improved_reflexes");
		assertRequirementsNotMet(check(create("magician"), "qi_focus", power));
		assertRequirementsNotMet(check(create("aspectedmagician"), "qi_focus", power));
		assertRequirementsMet(check(create("adept"), "qi_focus", power));
		assertRequirementsMet(check(create("mysticadept"), "qi_focus", power));
	}

	//-------------------------------------------------------------------
	@Test
	public void testMundaneCannotUseFoci() {
		assertRequirementsNotMet(check(create("mundane"), "power_focus"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testAstralSensitiveOnlyWeaponFocus() {
		KarmaCharacterGenerator charGen = create("mundane");
		SR6Quality sensitive = Shadowrun6Core.getItem(SR6Quality.class, "astral_sensitive");
		OperationResult<QualityValue> res = charGen.getQualityController().select(sensitive);
		assertTrue("Selecting quality failed: "+res, res.wasSuccessful());

		assertRequirementsMet(check(charGen, "weapon_focus", new Decision(WEAPON, UUID.randomUUID().toString())));
		assertRequirementsNotMet(check(charGen, "power_focus"));
		assertRequirementsNotMet(check(charGen, "qi_focus", new Decision(QI_POWER, "improved_reflexes")));
	}

}
