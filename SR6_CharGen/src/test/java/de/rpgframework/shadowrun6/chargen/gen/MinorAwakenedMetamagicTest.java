package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.chargen.BasicControllerEvents;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.MetamagicOrEcho;
import de.rpgframework.shadowrun.MetamagicOrEcho.Type;
import de.rpgframework.shadowrun.Priority;
import de.rpgframework.shadowrun.PriorityType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IMetatypeController;
import de.rpgframework.shadowrun.chargen.gen.PriorityTableController;
import de.rpgframework.shadowrun6.SR6MetaType;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.PriorityCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.SR6PrioritySettings;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-1029 follow-up: minor awakened characters must be offered initiations,
 * not transhumanism
 */
public class MinorAwakenedMetamagicTest {

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	private static void assertInitiationOffered(String when, SR6CharacterGenerator charGen) {
		assertEquals(when, "minor_awakened", charGen.getModel().getMagicOrResonanceType().getId());
		List<MetamagicOrEcho> avail = charGen.getMetamagicOrEchoController().getAvailable();
		assertTrue(when+": metamagic should be offered", avail.stream().anyMatch(m -> m.getType()==Type.METAMAGIC));
		assertFalse(when+": transhumanism should not be offered", avail.stream().anyMatch(m -> m.getType()==Type.TRANSHUMANISM));
	}

	//-------------------------------------------------------------------
	@Test
	public void testPriority() throws Exception {
		PriorityCharacterGenerator charGen = new PriorityCharacterGenerator();
		Shadowrun6Character model = new Shadowrun6Character();
		charGen.setModel(model, null);
		@SuppressWarnings("unchecked")
		PriorityTableController<Shadowrun6Character,SR6PrioritySettings> prio = charGen.getPriorityController();
		prio.setPriority(PriorityType.ATTRIBUTE, Priority.A);
		prio.setPriority(PriorityType.SKILLS, Priority.B);
		prio.setPriority(PriorityType.METATYPE, Priority.C);
		prio.setPriority(PriorityType.RESOURCES, Priority.D);
		prio.setPriority(PriorityType.MAGIC, Priority.E);
		IMetatypeController<SR6MetaType> meta = charGen.getMetatypeController();
		meta.select(Shadowrun6Core.getItem(SR6MetaType.class, "human"));
		charGen.runProcessors();

		SR6Quality sensitive = Shadowrun6Core.getItem(SR6Quality.class, "astral_sensitive");
		OperationResult<QualityValue> res = charGen.getQualityController().select(sensitive);
		assertTrue("Selecting failed: "+res, res.wasSuccessful());
		assertInitiationOffered("In wizard", charGen);

		charGen.runProcessors();
		assertInitiationOffered("After processing", charGen);

		// Leaving the wizard - reopen like GeneratorWrapper.setWrapped()
		byte[] raw = Shadowrun6Core.encode(model);
		Shadowrun6Character loaded = Shadowrun6Core.decode(raw);
		Shadowrun6Tools.resolveChar(loaded);
		PriorityCharacterGenerator reopened = new PriorityCharacterGenerator(loaded, null);
		reopened.setModel(loaded, null);
		reopened.runProcessors();
		assertInitiationOffered("Reopened", reopened);

		// Like WizardPageMagicOrResonance.refresh(): auto-select the only available type on every change
		reopened.addListener((type, param) -> {
			if (type!=BasicControllerEvents.CHARACTER_CHANGED) return;
			List<MagicOrResonanceType> avail = reopened.getMagicOrResonanceController().getAvailable();
			if (avail.size()==1)
				reopened.getMagicOrResonanceController().select(avail.get(0));
		});
		reopened.runProcessors();
		assertInitiationOffered("With wizard page listening", reopened);
	}

	//-------------------------------------------------------------------
	/** A real magic or resonance type has priority over minor_awakened - only mundane is ignored */
	@Test
	public void testRealTypeWinsOverMinorAwakened() {
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		Shadowrun6Character model = new Shadowrun6Character();
		charGen.setModel(model, null);
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mundane"));
		IMetatypeController<SR6MetaType> meta = charGen.getMetatypeController();
		meta.select(Shadowrun6Core.getItem(SR6MetaType.class, "human"));
		charGen.runProcessors();
		SR6Quality sensitive = Shadowrun6Core.getItem(SR6Quality.class, "astral_sensitive");
		assertTrue(charGen.getQualityController().select(sensitive).wasSuccessful());
		assertEquals("minor_awakened", model.getMagicOrResonanceType().getId());

		// Mundane is ignored
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mundane"));
		assertEquals("minor_awakened", model.getMagicOrResonanceType().getId());

		// Technomancer wins - also after recalculation
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "technomancer"));
		assertEquals("technomancer", model.getMagicOrResonanceType().getId());
		charGen.runProcessors();
		assertEquals("After processing", "technomancer", model.getMagicOrResonanceType().getId());
	}

}
