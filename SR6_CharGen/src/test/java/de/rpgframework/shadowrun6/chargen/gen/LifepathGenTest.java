package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.character.CharacterIOException;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ContactType;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.SkillType;
import de.rpgframework.shadowrun6.LifepathModule;
import de.rpgframework.shadowrun6.LifepathModuleValue;
import de.rpgframework.shadowrun6.PowerLevel;
import de.rpgframework.shadowrun6.SR6MetaType;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Rules;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.EarlyAdultGenerator;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.OneOrTwoQualityController;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathModuleGenerator;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathSurgeQualityController;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifepathCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;

/**
 * @author prelle
 *
 */
public class LifepathGenTest {

	private Shadowrun6Character model;
	private SR6LifepathCharacterGenerator charGen;

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Shadowrun6DataPlugin plugin = new Shadowrun6DataPlugin();
		plugin.init();

	}

	//-------------------------------------------------------------------
	@Before
	public void setup() {
		model = new Shadowrun6Character();
		charGen = new SR6LifepathCharacterGenerator(model,null);
		charGen.setModel(model, null);
	}

	//-------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testComp48() throws CharacterIOException {
		charGen.runProcessors();
		assertEquals(50, model.getKarmaFree());
//		SR6PointBuySettings settings = model.getCharGenSettings(SR6PointBuySettings.class);
//		assertEquals(100, settings.characterPoints);
		model.setName("Lucifer Lifepath");

		// Magic/Resonance
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mundane"));
		assertEquals(50, model.getKarmaFree());
//		assertEquals(2, model.getAttribute(ShadowrunAttribute.EDGE    ).getModifiedValue());

		SR6MetaType human = Shadowrun6Core.getItem(SR6MetaType.class, "troll");
		assertNotNull("No metatype controller found", charGen.getMetatypeController());
		charGen.getMetatypeController().canBeSelected(human);
		charGen.getMetatypeController().select(human);
		int expectedKarma = 50 - charGen.getMetatypeController().getKarmaCost(human);
		assertEquals(expectedKarma, model.getKarmaFree());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.AGILITY).getModifiedValue());
		assertEquals(2, model.getAttribute(ShadowrunAttribute.STRENGTH).getModifiedValue());
		assertEquals(2, model.getAttribute(ShadowrunAttribute.EDGE    ).getModifiedValue());

		/*
		 * The nationality will be UCAS, and our native language will be English. The quality
		 * this character feels they were born with is Tough as Nails at level 2, which costs us 8 Karma.
		 */
		charGen.getBornThisWayGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "built_tough"));
		charGen.getBornThisWayGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "sinner"));
		charGen.getBornThisWayGenerator().selectNativeLanguage("English");
		expectedKarma -= Shadowrun6Core.getItem(SR6Quality.class, "built_tough").getKarmaCost();
		expectedKarma += Shadowrun6Core.getItem(SR6Quality.class, "sinner").getKarmaCost();
		assertEquals(expectedKarma, model.getKarmaFree());
		assertTrue(model.hasQuality("built_tough"));
		assertTrue(model.hasQuality("sinner"));
		assertEquals(2, charGen.getBornThisWayGenerator().getQualityController().getSelected().size());

		// Modules
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		OperationResult<LifepathModuleValue> res = modules.select(Shadowrun6Core.getItem(LifepathModule.class, "gangmitglied"),
				new Decision("dd8bb000-fc65-4195-a8cf-1866fa01f2ed","REACTION"),
				new Decision("5e000fdb-8415-4a0c-9a1d-1e55f633ed34","firearms"),
				new Decision("72f10b55-0eb5-41b6-874a-00d3b738c905", "stealth"),
				new Decision("5fd30e6a-7d37-470f-9824-406f9494c5a6", "8c3920e9-06d7-4ae2-ae06-9c12b4b02e6e")
				);
		assertNotNull(res);
		assertTrue(res.getError(),res.wasSuccessful());

		charGen.finish();
		assertEquals(expectedKarma, model.getKarmaFree());
		assertEquals(50000, model.getNuyen());
		byte[] raw = Shadowrun6Core.encode(model);
		String xml = new String(raw);
		System.out.println(xml);
	}

	//-------------------------------------------------------------------
	@Test
	public void testLifepathModuleQualityChoicePolarity() {
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		LifepathModule money = Shadowrun6Core.getItem(LifepathModule.class, "an_eine_menge_geld_gekommen");
		assertNotNull(money);

		OperationResult<LifepathModuleValue> positive = modules.select(money,
				new Decision("eb8b1c35-686a-4251-a564-2fb29451a3a4", "built_tough")
				);
		assertFalse(positive.wasSuccessful());

		OperationResult<LifepathModuleValue> negative = modules.select(money,
				new Decision("eb8b1c35-686a-4251-a564-2fb29451a3a4", "sinner")
				);
		assertTrue(negative.getError(), negative.wasSuccessful());
	}

	//-------------------------------------------------------------------
	private final static String AGENT_SELMOD = "84cc83dc-7b58-45d1-811a-85ddc900fae1";
	private final static String AGENT_SPYCRAFT = "7dd367d3-df22-4ed9-805b-78f6250ae482";

	//-------------------------------------------------------------------
	private Decision[] agentDecisions(Decision...more) {
		List<Decision> ret = new ArrayList<>(List.of(
				new Decision("41d9d69d-6d3a-483d-98a4-243103671dc3", "LOGIC"),
				new Decision("82c68a8e-e879-4a44-9c54-56735b531885", "INTUITION"),
				new Decision("47e4b4de-7023-4214-8d3c-7152afaa4fb0", "perception")
				));
		ret.addAll(List.of(more));
		return ret.toArray(new Decision[ret.size()]);
	}

	//-------------------------------------------------------------------
	/**
	 * A life module with an undecided &lt;selmod&gt; cannot be selected
	 */
	@Test
	public void testLifepathModuleSelmodMustBeDecided() {
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		LifepathModule agent = Shadowrun6Core.getItem(LifepathModule.class, "agent");
		assertNotNull(agent);

		assertFalse(modules.canBeSelected(agent, agentDecisions()).get());
		assertFalse(modules.canBeSelected(agent, agentDecisions(new Decision(AGENT_SELMOD, "no-such-option"))).get());
		assertTrue(modules.canBeSelected(agent, agentDecisions(new Decision(AGENT_SELMOD, AGENT_SPYCRAFT))).get());
	}

	//-------------------------------------------------------------------
	/**
	 * Only the chosen option of a &lt;selmod&gt; is applied
	 */
	@Test
	public void testLifepathModuleSelmodGrantsChosenOption() {
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		LifepathModule agent = Shadowrun6Core.getItem(LifepathModule.class, "agent");
		charGen.runProcessors();
		List<String> before = knowledgeSkills();

		OperationResult<LifepathModuleValue> res = modules.select(agent, agentDecisions(new Decision(AGENT_SELMOD, AGENT_SPYCRAFT)));
		assertTrue(res.getError(), res.wasSuccessful());

		List<String> added = new ArrayList<>(knowledgeSkills());
		before.forEach(added::remove);
		assertEquals("Expected exactly the chosen knowledge skill, but got "+added, 1, added.size());
		assertEquals("Spionagetechniken", added.get(0));
		assertFalse(modules.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_LIFEPATH_SELMOD_MISSING.equals(todo.getKey())));
	}

	//-------------------------------------------------------------------
	/**
	 * Every &lt;selmod&gt; option of every life module can be told apart in the choice dialog
	 */
	@Test
	public void testLifepathModuleSelmodOptionsDistinguishable() {
		List<String> problems = new ArrayList<>();
		for (LifepathModule module : Shadowrun6Core.getItemList(LifepathModule.class)) {
			for (Modification mod : module.getOutgoingModifications()) {
				if (!(mod instanceof ModificationChoice selMod))
					continue;
				List<String> labels = new ArrayList<>();
				for (Modification opt : selMod.getModificiations()) {
					if (!(opt instanceof DataItemModification dataMod) || dataMod.getId()==null) {
						problems.add(module.getId()+": option without id "+opt);
						continue;
					}
					// TODO: technomancerin and thinktank still have "i18n.?" placeholders instead of knowledge skill names
					if (dataMod.getDecisions().stream().anyMatch(dec -> "i18n.?".equals(dec.getValue())))
						continue;
					labels.add(Shadowrun6Tools.getModificationString(opt, Locale.GERMAN));
				}
				if (labels.contains(null) || labels.stream().distinct().count()<labels.size())
					problems.add(module.getId()+": "+labels);
			}
		}
		assertTrue(problems.size()+" problems:\n"+String.join("\n", problems), problems.isEmpty());
	}

	//-------------------------------------------------------------------
	/**
	 * A previously selected module with an undecided &lt;selmod&gt; (e.g. from an older save) creates a ToDo
	 */
	@Test
	public void testLifepathModuleUndecidedSelmodCreatesToDo() {
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		LifepathModule agent = Shadowrun6Core.getItem(LifepathModule.class, "agent");
		LifepathModuleValue val = new LifepathModuleValue(agent);
		for (Decision dec : agentDecisions())
			val.addDecision(dec);
		charGen.getSettings().addModule(val);
		charGen.runProcessors();

		assertTrue(modules.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_LIFEPATH_SELMOD_MISSING.equals(todo.getKey())));
	}

	//-------------------------------------------------------------------
	private List<String> knowledgeSkills() {
		return model.getSkillValues(SkillType.KNOWLEDGE).stream()
				.map(val -> val.getNameWithoutRating(Locale.GERMAN))
				.toList();
	}

	//-------------------------------------------------------------------
	private SR6Quality quality(String id) {
		SR6Quality ret = Shadowrun6Core.getItem(SR6Quality.class, id);
		assertNotNull("Unknown quality "+id, ret);
		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * Born this way: one or two qualities - if two, one positive and one negative
	 */
	@Test
	public void testBornQualitiesOnePositiveOneNegative() {
		charGen.runProcessors();
		OneOrTwoQualityController ctrl = charGen.getBornThisWayGenerator().getQualityController();
		assertEquals(2, charGen.getBornThisWayGenerator().getMaximumBornQualities());

		assertTrue(ctrl.select(quality("built_tough")).wasSuccessful());
		assertFalse("Second positive quality allowed", ctrl.canBeSelected(quality("ambidextrous")).get());
		assertFalse(ctrl.select(quality("ambidextrous")).wasSuccessful());
		assertTrue(ctrl.select(quality("sinner")).wasSuccessful());
		assertFalse("Third quality allowed", ctrl.canBeSelected(quality("allergy")).get());
		assertEquals(2, charGen.getSettings().getBornQualities().size());
	}

	//-------------------------------------------------------------------
	@Test
	public void testChildhoodQualitiesOnePositiveOneNegative() {
		charGen.runProcessors();
		OneOrTwoQualityController ctrl = charGen.getChildhoodGenerator().getQualityController();

		assertTrue(ctrl.select(quality("sinner")).wasSuccessful());
		assertFalse("Second negative quality allowed", ctrl.canBeSelected(quality("allergy")).get());
		assertTrue(ctrl.select(quality("ambidextrous")).wasSuccessful());
		assertFalse("Third quality allowed", ctrl.canBeSelected(quality("analytical_mind")).get());
	}

	//-------------------------------------------------------------------
	/**
	 * Characters saved before the rule was enforced get a ToDo
	 */
	@Test
	public void testBornQualitiesSamePolarityCreatesToDo() {
		charGen.getSettings().addBornQuality(quality("built_tough"));
		charGen.getSettings().addBornQuality(quality("ambidextrous"));
		charGen.runProcessors();

		assertTrue(charGen.getBornThisWayGenerator().getToDos().stream()
				.anyMatch(todo -> SR6RejectReasons.TODO_LIFEPATH_QUALITY_POLARITY.equals(todo.getKey())));
	}

	//-------------------------------------------------------------------
	/**
	 * Coming of age: one or two qualities in any combination - unlike childhood both may be positive (or negative)
	 */
	@Test
	public void testComingOfAgeQualitiesMayHaveSamePolarity() {
		charGen.runProcessors();
		EarlyAdultGenerator early = charGen.getEarlyAdultGenerator();
		assertTrue(early.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_EARLY_QUALITY_MISSING.equals(todo.getKey())));

		OneOrTwoQualityController ctrl = early.getQualityController();
		assertTrue(ctrl.select(quality("ambidextrous")).wasSuccessful());
		assertTrue("Second positive quality refused", ctrl.select(quality("analytical_mind")).wasSuccessful());
		assertFalse("Third quality allowed", ctrl.canBeSelected(quality("sinner")).get());

		assertTrue(model.hasQuality("ambidextrous"));
		assertTrue(model.hasQuality("analytical_mind"));
		assertFalse(early.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_EARLY_QUALITY_MISSING.equals(todo.getKey())));
	}

	//-------------------------------------------------------------------
	/**
	 * Coming of age: any combination is fine - one positive and one negative as well as two negative
	 */
	@Test
	public void testComingOfAgeQualitiesAnyCombination() {
		charGen.runProcessors();
		OneOrTwoQualityController ctrl = charGen.getEarlyAdultGenerator().getQualityController();
		assertTrue(ctrl.select(quality("ambidextrous")).wasSuccessful());
		assertTrue("Positive + negative refused", ctrl.select(quality("sinner")).wasSuccessful());
		assertEquals(2, ctrl.getSelected().size());

		ctrl.deselect(model.getQuality("ambidextrous"));
		assertTrue("Negative + negative refused", ctrl.select(quality("allergy"),
				new Decision(quality("allergy").getChoices().get(0).getUUID(),"seasonal"),
				new Decision(quality("allergy").getChoices().get(1).getUUID(),"mild"),
				new Decision(quality("allergy").getChoices().get(2).getUUID(),"Pollen")
				).wasSuccessful());
		assertEquals(2, ctrl.getSelected().size());
	}

	//-------------------------------------------------------------------
	/**
	 * Coming of age qualities must not overwrite those from childhood
	 */
	@Test
	public void testComingOfAgeQualitiesSeparateFromChildhood() {
		charGen.runProcessors();
		assertTrue(charGen.getChildhoodGenerator().getQualityController().select(quality("sinner")).wasSuccessful());
		assertTrue(charGen.getEarlyAdultGenerator().getQualityController().select(quality("built_tough")).wasSuccessful());

		assertEquals("sinner", charGen.getSettings().getChildhoodQuality1().getId());
		assertNull(charGen.getSettings().getChildhoodQuality2());
		assertEquals("built_tough", charGen.getSettings().getEarlyAdultQuality1().getId());
		assertEquals(1, charGen.getEarlyAdultGenerator().getQualityController().getSelected().size());
		assertEquals(1, charGen.getChildhoodGenerator().getQualityController().getSelected().size());
	}

	//-------------------------------------------------------------------
	/**
	 * Coming of age: one connection of any type with 4 points on influence and loyalty, each at least 1
	 */
	@Test
	public void testComingOfAgeConnection() {
		charGen.runProcessors();
		EarlyAdultGenerator early = charGen.getEarlyAdultGenerator();
		assertTrue(early.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_EARLY_CONTACT_MISSING.equals(todo.getKey())));
		assertEquals(10, early.getAvailableContactTypes().size());
		assertFalse(early.getAvailableContactTypes().stream().anyMatch(type -> type.getId().equals("FREE_SPIRIT") || type.getId().equals("EXISTING")));

		ContactType media = Shadowrun6Core.getItem(ContactType.class, "MEDIA");
		Contact contact = early.createContact(media);
		assertNotNull(contact);
		assertTrue(model.getContacts().contains(contact));
		assertEquals(4, contact.getRating()+contact.getLoyalty());
		assertSame("Second connection created", contact, early.createContact(media));
		assertEquals(1, model.getContacts().size());

		assertTrue(early.setContactInfluence(3));
		assertEquals(3, contact.getRating());
		assertEquals(1, contact.getLoyalty());
		assertFalse("Loyalty 0 allowed", early.setContactInfluence(4));
		assertFalse("Influence 0 allowed", early.setContactInfluence(0));
		assertFalse(early.setContactType(Shadowrun6Core.getItem(ContactType.class, "FREE_SPIRIT")));
		assertEquals("MEDIA", contact.getType().getId());
		assertFalse(early.getToDos().stream().anyMatch(todo -> todo.getKey().startsWith("todo.lifepath.early.contact")));

		// Changed elsewhere (e.g. on the contacts page)
		contact.setLoyalty(3);
		charGen.runProcessors();
		assertTrue(early.getToDos().stream().anyMatch(todo -> SR6RejectReasons.TODO_EARLY_CONTACT_POINTS.equals(todo.getKey())));
	}

	//-------------------------------------------------------------------
	@Test
	public void testCannotDeselectRequiredLifeModule() {
		SR6LifePathModuleGenerator modules = charGen.getModuleGenerator();
		LifepathModule studies = Shadowrun6Core.getItem(LifepathModule.class, "studium");
		LifepathModule doctor = Shadowrun6Core.getItem(LifepathModule.class, "aerztin");
		assertNotNull(studies);
		assertNotNull(doctor);

		OperationResult<LifepathModuleValue> studiesResult = modules.select(studies,
				new Decision("4085c382-f35f-4cf4-a3a0-1f1de3b08e83", "LOGIC"),
				new Decision("d3782337-10d5-4be6-b7ee-d8f07098537b", "biotech"),
				new Decision("5e39d6f5-b06f-457c-8017-e2cf6e3e9c5d", "electronics"),
				new Decision("39650ed7-5e36-4a4b-a862-8f5471084966", "a44cac60-5483-4def-b749-f41a21d374bd"),
				new Decision("272e3c47-d229-4751-ad19-c3171422d881", "f6cfc352-0bbd-421b-a783-bbbb07a7bf42")
				);
		assertTrue(studiesResult.getError(), studiesResult.wasSuccessful());

		OperationResult<LifepathModuleValue> doctorResult = modules.select(doctor,
				new Decision("3a2d5141-0787-4d99-b550-bf5d90a8ec74", "LOGIC"),
				new Decision("d6a6b973-7455-4d5f-877a-db1db4db542f", "biotech"),
				new Decision("8f4bd0e0-dd11-4055-8d22-014bdd1a9c46", "e3ad4529-c859-4159-867d-ffdd6a6b2a50")
				);
		assertTrue(doctorResult.getError(), doctorResult.wasSuccessful());

		assertFalse(modules.canBeDeselected(studiesResult.get()).get());
		assertFalse(modules.deselect(studiesResult.get()));
		assertTrue(modules.getSelected().contains(studiesResult.get()));

		assertTrue(modules.deselect(doctorResult.get()));
		assertTrue(modules.canBeDeselected(studiesResult.get()).get());
		assertTrue(modules.deselect(studiesResult.get()));
	}

	//-------------------------------------------------------------------
	@Test
	public void testSurgeQualitySurvivesLifePathReset() {
		charGen.runProcessors();
		charGen.getBornThisWayGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "sinner"));
		charGen.getBornThisWayGenerator().selectNativeLanguage("German");
		int baseKarma = model.getKarmaFree();

		SR6LifePathSurgeQualityController surge = new SR6LifePathSurgeQualityController(charGen);
		SR6Quality quills = Shadowrun6Core.getItem(SR6Quality.class, "quills");
		assertNotNull(quills);
		SR6Quality asymmetry = Shadowrun6Core.getItem(SR6Quality.class, "asymmetric_deformity");
		assertNotNull(asymmetry);

		OperationResult<?> result = surge.select(quills);
		assertTrue(result.getError(), result.wasSuccessful());
		assertTrue(model.hasQuality("quills"));
		assertEquals(1, surge.getSelected().size());
		assertEquals(Math.abs(quills.getKarmaCost()), surge.getKarmaForSURGE());
		assertEquals(baseKarma - quills.getKarmaCost(), model.getKarmaFree());

		result = surge.select(asymmetry,
				new Decision(asymmetry.getChoices().get(0).getUUID(), "picasso"));
		assertTrue(result.getError(), result.wasSuccessful());
		assertTrue(model.hasQuality("asymmetric_deformity"));
		assertEquals(quills.getKarmaCost() - asymmetry.getKarmaCost(), surge.getKarmaForSURGE());
		assertEquals(baseKarma - quills.getKarmaCost() + asymmetry.getKarmaCost(), model.getKarmaFree());

		charGen.runProcessors();
		assertTrue(model.hasQuality("quills"));
		assertTrue(model.hasQuality("asymmetric_deformity"));
		assertEquals(2, surge.getSelected().size());
		assertEquals(quills.getKarmaCost() - asymmetry.getKarmaCost(), surge.getKarmaForSURGE());
		assertEquals(baseKarma - quills.getKarmaCost() + asymmetry.getKarmaCost(), model.getKarmaFree());
	}

	//-------------------------------------------------------------------
	@Test
	public void testNegativeSurgeQualityGrantsGlobalKarma() {
		charGen.runProcessors();
		int baseKarma = model.getKarmaFree();

		SR6LifePathSurgeQualityController surge = new SR6LifePathSurgeQualityController(charGen);
		SR6Quality asymmetry = Shadowrun6Core.getItem(SR6Quality.class, "asymmetric_deformity");
		assertNotNull(asymmetry);

		OperationResult<?> result = surge.select(asymmetry,
				new Decision(asymmetry.getChoices().get(0).getUUID(), "picasso"));
		assertTrue(result.getError(), result.wasSuccessful());
		assertTrue(model.hasQuality("asymmetric_deformity"));
		assertEquals(-asymmetry.getKarmaCost(), surge.getKarmaForSURGE());
		assertEquals(baseKarma + asymmetry.getKarmaCost(), model.getKarmaFree());
	}

	//-------------------------------------------------------------------
	@Test
	public void testLifePathPowerLevelOnlyChangesModuleCount() {
		assertLifePathDefaults(PowerLevel.STREET_LEVEL, 6);
		assertLifePathDefaults(PowerLevel.STANDARD, 8);
		assertLifePathDefaults(PowerLevel.ELITE, 10);
	}

	//-------------------------------------------------------------------
	private void assertLifePathDefaults(PowerLevel level, int expectedModules) {
		charGen.applyPowerLevelDefaults(level);
		assertEquals(50, charGen.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_ADJUSTMENT_KARMA));
		assertEquals(expectedModules, charGen.getLifePathMaximumModules());
		assertEquals(6, charGen.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_MAX_QUALITIES));
		assertEquals(20, charGen.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_NEGATIVE_KARMA_CAP));
		assertEquals(25000, charGen.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_START_NUYEN));
	}

	//-------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testGaius() throws CharacterIOException {
		charGen.runProcessors();
		assertEquals(50, model.getKarmaFree());
//		SR6PointBuySettings settings = model.getCharGenSettings(SR6PointBuySettings.class);
//		assertEquals(100, settings.characterPoints);
		model.setName("Gaius");

		// Magic/Resonance
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mysticadept"));
		assertEquals(50, model.getKarmaFree());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.AGILITY).getModifiedValue());
		assertEquals(2, model.getAttribute(ShadowrunAttribute.EDGE    ).getModifiedValue());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.MAGIC   ).getModifiedValue());

		SR6MetaType human = Shadowrun6Core.getItem(SR6MetaType.class, "elf");
		assertNotNull("No metatype controller found", charGen.getMetatypeController());
		charGen.getMetatypeController().canBeSelected(human);
		charGen.getMetatypeController().select(human);
		assertEquals(50, model.getKarmaFree());
		assertEquals(2, model.getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue());
		assertEquals(2, model.getAttribute(ShadowrunAttribute.AGILITY).getModifiedValue());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.EDGE    ).getModifiedValue());
		assertEquals(1, model.getAttribute(ShadowrunAttribute.MAGIC   ).getModifiedValue());
		charGen.getBornThisWayGenerator().selectNativeLanguage("German");
		charGen.getBornThisWayGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "sinner"));

		charGen.getChildhoodGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "con"));
		charGen.getChildhoodGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "influence"));
		charGen.getChildhoodGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "perception"));
		charGen.getChildhoodGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "athletics"));
		charGen.getChildhoodGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "charismatic_defense"));
		charGen.getChildhoodGenerator().getQualityController().select(Shadowrun6Core.getItem(SR6Quality.class, "allergy"),
				new Decision(Shadowrun6Core.getItem(SR6Quality.class, "allergy").getChoices().get(0).getUUID(),"seasonal"),
				new Decision(Shadowrun6Core.getItem(SR6Quality.class, "allergy").getChoices().get(1).getUUID(),"mild"),
				new Decision(Shadowrun6Core.getItem(SR6Quality.class, "allergy").getChoices().get(2).getUUID(),"Pollen")
				);

		charGen.getEarlyAdultGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "sorcery"));

		charGen.finish();
		byte[] raw = Shadowrun6Core.encode(model);
		String xml = new String(raw);
		System.out.println(xml);
	}
}
