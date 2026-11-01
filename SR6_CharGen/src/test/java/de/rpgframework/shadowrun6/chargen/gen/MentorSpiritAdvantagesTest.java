package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.shadowrun.AdeptPowerValue;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-252 (Part 3): Mystic adepts choose between magician and adept
 * advantages of their mentor spirit
 */
public class MentorSpiritAdvantagesTest {

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init();
	}

	//-------------------------------------------------------------------
	private KarmaCharacterGenerator createMysticAdept(Shadowrun6Character model) {
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		charGen.setModel(model, null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mysticadept"));
		return charGen;
	}

	//-------------------------------------------------------------------
	private QualityValue selectSeducer(KarmaCharacterGenerator charGen, String advantages) {
		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		OperationResult<QualityValue> res = charGen.getQualityController().select(mentor,
				new Decision(mentor.getChoices().get(0).getUUID(), "seducer"),
				new Decision(QualityValue.MENTOR_SPIRIT_ADVANTAGES, advantages));
		assertTrue("Selecting mentor spirit failed: "+res, res.wasSuccessful());
		charGen.runProcessors();
		return res.get();
	}

	//-------------------------------------------------------------------
	@Test
	public void testMagicianAdvantagesNeedNoAdeptChoice() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createMysticAdept(model);
		selectSeducer(charGen, "magician");

		assertNull("Adept power from mentor spirit granted for magician advantages", model.getAdeptPower("improved_ability"));
	}

	//-------------------------------------------------------------------
	/**
	 * COMMBUG-220: The skill chosen for a rated adept power with a choice
	 * must reach the granted power
	 */
	@Test
	public void testAdeptPowerChoiceFromMentorSpirit() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		charGen.setModel(model, null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "adept"));
		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		OperationResult<QualityValue> res = charGen.getQualityController().select(mentor,
				new Decision(mentor.getChoices().get(0).getUUID(), "wise_warrior"),
				new Decision(UUID.fromString("97fb1dcd-14e4-456c-aad7-17a7bf8cbbae"), "firearms"));
		assertTrue("Selecting mentor spirit failed: "+res, res.wasSuccessful());
		charGen.runProcessors();

		AdeptPowerValue power = model.getAdeptPower("improved_ability_combat");
		assertNotNull("Adept power from mentor spirit missing", power);
		Decision dec = power.getDecision(UUID.fromString("97fb1dcd-14e4-456c-aad7-17a7bf8cbbae"));
		assertNotNull("Skill decision did not reach the adept power", dec);
		assertEquals("firearms", dec.getValue());
	}

	//-------------------------------------------------------------------
	@Test
	public void testUndecidedKeepsBothAdvantages() {
		// Characters saved before the decision existed keep their old result
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createMysticAdept(model);
		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		OperationResult<QualityValue> res = charGen.getQualityController().select(mentor,
				new Decision(mentor.getChoices().get(0).getUUID(), "seducer"));
		assertTrue("Selecting mentor spirit failed: "+res, res.wasSuccessful());
		charGen.runProcessors();

		assertNotNull("Adept power from mentor spirit missing", model.getAdeptPower("improved_ability"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testAdeptAdvantages() {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = createMysticAdept(model);
		selectSeducer(charGen, "adept");

		assertNotNull("Adept power from mentor spirit missing", model.getAdeptPower("improved_ability"));
	}

}
