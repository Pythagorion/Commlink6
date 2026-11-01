package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

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
 * COM6-252 (Part 1): Rated adept powers granted by mentor spirits
 */
public class MentorSpiritAdeptPowerTest {

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init();
	}

	//-------------------------------------------------------------------
	@Test
	public void testEagleGrantsCombatSense1() {
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		Shadowrun6Character model = new Shadowrun6Character();
		charGen.setModel(model, null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "adept"));

		SR6Quality mentor = Shadowrun6Core.getItem(SR6Quality.class, "mentor_spirit");
		OperationResult<QualityValue> res = charGen.getQualityController().select(mentor,
				new Decision(mentor.getChoices().get(0).getUUID(), "eagle"));
		assertTrue("Selecting mentor spirit failed: "+res, res.wasSuccessful());
		charGen.runProcessors();

		AdeptPowerValue power = model.getAdeptPower("combat_sense");
		assertNotNull("Combat Sense not granted", power);
		System.out.println("COMBAT_SENSE distributed="+power.getDistributed()+" modified="+power.getModifiedValue()+" incoming="+power.getIncomingModifications()
			+" ctrlValue="+charGen.getAdeptPowerController().getValue(power)
			+" inList="+model.getAdeptPowers().stream().filter(p -> p.getKey().equals("combat_sense")).map(p -> p.getModifiedValue()+"/"+(p==power)).toList());
		assertEquals(0, power.getDistributed());
		assertEquals(1, power.getModifiedValue());
		assertEquals(1, charGen.getAdeptPowerController().getValue(power));
	}

}
