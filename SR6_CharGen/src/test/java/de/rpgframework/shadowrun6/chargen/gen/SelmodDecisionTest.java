package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.shadowrun.AdeptPower;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.MentorSpirit;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6RuleFlag;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-252 (Part 2): Decisions for &lt;selmod&gt; are resolved by the option id
 */
public class SelmodDecisionTest {

	// Jester (Street Wyrd): <selmod> empathic_sense OR enthralling_performance
	private final static UUID JESTER_SELMOD = UUID.fromString("25d96a27-33d4-4540-8ec6-2ab82d983a2b");
	private final static UUID JESTER_ENTHRALLING = UUID.fromString("9ed39ee1-d25f-407b-b86f-45ffd6399e00");
	// Beast's Way (Street Wyrd): <selmod> wild die on chosen skill OR MENTOR_SPIRIT_BOTH
	private final static UUID BEAST_SKILL_CHOICE = UUID.fromString("e26e2313-adf9-494e-82be-62387e0d4efd");
	private final static UUID BEAST_SELMOD = UUID.fromString("d4400bf0-b64a-4209-875e-a6d2cb1bb2a9");
	private final static UUID BEAST_RULE = UUID.fromString("c111ed8c-0b14-4e82-9a2d-b01bbafd5bc1");
	private final static UUID BEAST_WILD = UUID.fromString("fc64e152-318c-425d-aad3-5ce9de7e19b2");

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init();
	}

	//-------------------------------------------------------------------
	private KarmaCharacterGenerator createAdept(Shadowrun6Character model) {
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		charGen.setModel(model, null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "adept"));
		return charGen;
	}

	//-------------------------------------------------------------------
	private QualityValue selectQuality(KarmaCharacterGenerator charGen, String id, Decision...decisions) {
		SR6Quality qual = Shadowrun6Core.getItem(SR6Quality.class, id);
		assertNotNull("Unknown quality "+id, qual);
		OperationResult<QualityValue> res = charGen.getQualityController().select(qual, decisions);
		assertTrue("Selecting "+id+" failed: "+res, res.wasSuccessful());
		charGen.runProcessors();
		return res.get();
	}

	//-------------------------------------------------------------------
	@Test
	public void testMentorSpiritSelmodDecided() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createAdept(model);
		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		QualityValue value = selectQuality(charGen, "mentor_spirit",
				new Decision(mentor.getChoices().get(0).getUUID(), "jester"),
				new Decision(JESTER_SELMOD, JESTER_ENTHRALLING));

		assertNotNull("Chosen adept power not granted", model.getAdeptPower("enthralling_performance"));
		assertNull("Not chosen adept power granted", model.getAdeptPower("empathic_sense"));

		// The decision is displayed with the name of the chosen power
		String decString = value.getDecisionString(Locale.ENGLISH, model);
		String powerName = Shadowrun6Core.getItem(AdeptPower.class, "enthralling_performance").getName(Locale.ENGLISH);
		assertTrue("Power name missing in: "+decString, decString.contains(powerName));
		assertFalse("Option id displayed in: "+decString, decString.contains(JESTER_ENTHRALLING.toString()));
	}

	//-------------------------------------------------------------------
	@Test
	public void testSelmodOptionsDisplayable() {
		MentorSpirit jester = Shadowrun6Core.getItem(MentorSpirit.class, "jester");
		ModificationChoice selMod = (ModificationChoice) jester.getAdeptModifications().stream()
			.filter(m -> m instanceof ModificationChoice)
			.findFirst().orElseThrow();
		for (Modification opt : selMod) {
			assertNotNull("No display string for "+opt, Shadowrun6Tools.getModificationString(opt, Locale.ENGLISH));
		}
	}

	//-------------------------------------------------------------------
	@Test
	public void testMentorSpiritSelmodUndecided() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createAdept(model);
		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		selectQuality(charGen, "mentor_spirit",
				new Decision(mentor.getChoices().get(0).getUUID(), "jester"));

		assertNull(model.getAdeptPower("enthralling_performance"));
		assertNull(model.getAdeptPower("empathic_sense"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testQualitySelmodChoosesRule() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createAdept(model);
		selectQuality(charGen, "beasts_way",
				new Decision(BEAST_SKILL_CHOICE, "perception"),
				new Decision(BEAST_SELMOD, BEAST_RULE));

		assertTrue("MENTOR_SPIRIT_BOTH not set", model.hasRuleFlag(SR6RuleFlag.MENTOR_SPIRIT_BOTH));
	}

	//-------------------------------------------------------------------
	@Test
	public void testQualitySelmodChoosesOther() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createAdept(model);
		selectQuality(charGen, "beasts_way",
				new Decision(BEAST_SKILL_CHOICE, "perception"),
				new Decision(BEAST_SELMOD, BEAST_WILD));

		assertFalse("MENTOR_SPIRIT_BOTH set although not chosen", model.hasRuleFlag(SR6RuleFlag.MENTOR_SPIRIT_BOTH));
	}

}
