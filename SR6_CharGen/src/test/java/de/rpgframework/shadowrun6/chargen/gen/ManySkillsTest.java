package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.Priority;
import de.rpgframework.shadowrun.PriorityType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IMetatypeController;
import de.rpgframework.shadowrun.chargen.gen.PriorityTableController;
import de.rpgframework.shadowrun6.SR6MetaType;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.SR6SkillValue;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6SkillController;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.pointbuy.PointBuyCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.PriorityCharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.priority.SR6PrioritySettings;
import de.rpgframework.shadowrun6.chargen.gen.priority.SumToTenCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * COM6-941: Without "Many Skills" only one skill may be maxed during creation,
 * with it two skills.
 */
public class ManySkillsTest {

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	@Test
	public void testPriority() {
		runScenario(PriorityCharacterGenerator::new, ManySkillsTest::preparePriority);
	}

	//-------------------------------------------------------------------
	@Test
	public void testSumToTen() {
		runScenario(SumToTenCharacterGenerator::new, ManySkillsTest::preparePriority);
	}

	//-------------------------------------------------------------------
	@Test
	public void testKarma() {
		runScenario(KarmaCharacterGenerator::new, ManySkillsTest::prepareMundane);
	}

	//-------------------------------------------------------------------
	@Test
	public void testPointBuy() {
		runScenario(PointBuyCharacterGenerator::new, ManySkillsTest::prepareMundane);
	}

	//-------------------------------------------------------------------
	private static void preparePriority(SR6CharacterGenerator charGen) {
		@SuppressWarnings("unchecked")
		PriorityTableController<Shadowrun6Character,SR6PrioritySettings> prio = ((PriorityCharacterGenerator)charGen).getPriorityController();
		prio.setPriority(PriorityType.SKILLS, Priority.A);
		prio.setPriority(PriorityType.ATTRIBUTE, Priority.B);
		prio.setPriority(PriorityType.METATYPE, Priority.C);
		prio.setPriority(PriorityType.RESOURCES, Priority.D);
		prio.setPriority(PriorityType.MAGIC, Priority.E);
	}

	//-------------------------------------------------------------------
	private static void prepareMundane(SR6CharacterGenerator charGen) {
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mundane"));
	}

	//-------------------------------------------------------------------
	private void runScenario(Supplier<? extends SR6CharacterGenerator> newGenerator, Consumer<SR6CharacterGenerator> prepare) {
		SR6CharacterGenerator charGen = newGenerator.get();
		Shadowrun6Character model = new Shadowrun6Character();
		charGen.setModel(model, null);
		prepare.accept(charGen);
		IMetatypeController<SR6MetaType> meta = charGen.getMetatypeController();
		meta.select(Shadowrun6Core.getItem(SR6MetaType.class, "human"));
		charGen.runProcessors();

		SR6SkillController skills = charGen.getSkillController();
		SR6SkillValue athletics = selectAndRaise(skills, "athletics", 6);
		assertEquals(6, athletics.getDistributed());
		// Without many_skills a second skill may not be maxed
		SR6SkillValue firearms = selectAndRaise(skills, "firearms", 5);
		Possible poss = skills.canBeIncreased(firearms);
		assertFalse("Second skill may not be maxed without 'Many Skills'", poss.get());

		// Select many_skills
		SR6Quality manySkills = Shadowrun6Core.getItem(SR6Quality.class, "many_skills");
		assertNotNull(manySkills);
		OperationResult<QualityValue> res = charGen.getQualityController().select(manySkills);
		assertTrue("Selecting 'Many Skills' failed: "+res, res.wasSuccessful());

		poss = skills.canBeIncreased(firearms);
		assertTrue("Second skill should be allowed to max with 'Many Skills': "+poss, poss.get());
		assertTrue(skills.increase(firearms).wasSuccessful());
		assertEquals(6, firearms.getDistributed());

		// But not a third one
		SR6SkillValue stealth = selectAndRaise(skills, "stealth", 5);
		assertFalse("Third skill may not be maxed with 'Many Skills'", skills.canBeIncreased(stealth).get());
	}

	//-------------------------------------------------------------------
	private static SR6SkillValue selectAndRaise(SR6SkillController skills, String id, int target) {
		SR6Skill skill = Shadowrun6Core.getSkill(id);
		OperationResult<SR6SkillValue> sel = skills.select(skill);
		assertTrue("Selecting "+id+" failed: "+sel, sel.wasSuccessful());
		SR6SkillValue val = sel.get();
		while (val.getDistributed()<target) {
			Possible poss = skills.canBeIncreased(val);
			assertTrue("Cannot raise "+id+" beyond "+val.getDistributed()+": "+poss, poss.get());
			assertTrue(skills.increase(val).wasSuccessful());
		}
		return val;
	}

}
