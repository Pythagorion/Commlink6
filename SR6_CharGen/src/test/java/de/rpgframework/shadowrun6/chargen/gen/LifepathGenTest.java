package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.character.CharacterIOException;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.ToDoElement.Severity;
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
import de.rpgframework.shadowrun6.SR6SkillValue;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Rules;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.chargen.gen.SR6ContactGenerator;
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
	/**
	 * Select a life module with decisions given as "choice-UUID=value"
	 */
	private LifepathModuleValue selectModule(String id, String... decisions) {
		LifepathModule module = Shadowrun6Core.getItem(LifepathModule.class, id);
		assertNotNull("Unknown life module "+id, module);
		Decision[] decs = new Decision[decisions.length];
		for (int i=0; i<decisions.length; i++) {
			String[] pair = decisions[i].split("=", 2);
			decs[i] = new Decision(pair[0], pair[1]);
		}
		OperationResult<LifepathModuleValue> res = charGen.getModuleGenerator().select(module, decs);
		assertTrue("Selecting "+id+" failed: "+res.getError(), res.wasSuccessful());
		return res.get();
	}

	//-------------------------------------------------------------------
	private int attr(ShadowrunAttribute key) {
		return model.getAttribute(key).getModifiedValue();
	}

	//-------------------------------------------------------------------
	private int skill(String id) {
		SR6SkillValue val = model.getSkillValue(Shadowrun6Core.getSkill(id));
		return (val!=null)?val.getModifiedValue():0;
	}

	//-------------------------------------------------------------------
	private void assertNoStopper(String step, List<ToDoElement> todos) {
		List<String> stoppers = todos.stream()
				.filter(todo -> todo.getSeverity()==Severity.STOPPER)
				.map(todo -> todo.getKey()+": "+todo.getMessage(Locale.ENGLISH))
				.toList();
		assertTrue(step+" has open ToDos: "+stoppers, stoppers.isEmpty());
	}

	//-------------------------------------------------------------------
	/**
	 * A complete life path character - all decisions of all life stages are made
	 * and the calculated character is checked against what the decisions must cause.
	 */
	@Test
	public void testGaius() throws CharacterIOException {
		charGen.runProcessors();
		assertEquals(50, model.getKarmaFree());
		model.setName("Gaius");

		// Magic/Resonance
		charGen.getMagicOrResonanceController().select(Shadowrun6Core.getItem(MagicOrResonanceType.class, "mysticadept"));
		assertEquals(50, model.getKarmaFree());
		assertEquals(1, attr(ShadowrunAttribute.CHARISMA));
		assertEquals(1, attr(ShadowrunAttribute.AGILITY));
		assertEquals(2, attr(ShadowrunAttribute.EDGE));
		assertEquals(1, attr(ShadowrunAttribute.MAGIC));

		// Metatype
		SR6MetaType elf = Shadowrun6Core.getItem(SR6MetaType.class, "elf");
		assertNotNull("No metatype controller found", charGen.getMetatypeController());
		assertTrue(charGen.getMetatypeController().select(elf));
		int expectedKarma = 50 - charGen.getMetatypeController().getKarmaCost(elf);
		assertEquals(expectedKarma, model.getKarmaFree());
		assertEquals(2, attr(ShadowrunAttribute.CHARISMA));
		assertEquals(2, attr(ShadowrunAttribute.AGILITY));
		assertEquals(1, attr(ShadowrunAttribute.EDGE));
		assertEquals(1, attr(ShadowrunAttribute.MAGIC));

		/*
		 * Born this way: native language and one quality
		 */
		charGen.getBornThisWayGenerator().selectNativeLanguage("German");
		assertTrue(charGen.getBornThisWayGenerator().getQualityController().select(quality("sinner")).wasSuccessful());
		expectedKarma += quality("sinner").getKarmaCost();
		assertEquals(expectedKarma, model.getKarmaFree());
		assertEquals(4, skill("language"));
		model.setLifepathBabyBackground("Born in Hamburg");

		/*
		 * Early childhood: 4 skills, the area grown up in and a positive and a negative quality
		 */
		for (String id : List.of("con","influence","perception","athletics"))
			charGen.getChildhoodGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, id));
		charGen.getChildhoodGenerator().selectChildhoodArea("Hamburg");
		assertTrue(charGen.getChildhoodGenerator().getQualityController().select(quality("charismatic_defense")).wasSuccessful());
		assertTrue(charGen.getChildhoodGenerator().getQualityController().select(quality("allergy"),
				new Decision(quality("allergy").getChoices().get(0).getUUID(),"seasonal"),
				new Decision(quality("allergy").getChoices().get(1).getUUID(),"mild"),
				new Decision(quality("allergy").getChoices().get(2).getUUID(),"Pollen")
				).wasSuccessful());
		model.setLifepathChildhoodBackground("Grew up on the streets of Hamburg");
		for (String id : List.of("con","influence","perception","athletics"))
			assertEquals("Childhood skill "+id, 2, skill(id));

		/*
		 * Coming of age: focus skill, attribute, a quality and the connection
		 */
		// Mystic adepts already have sorcery 1 from their magic type
		int sorceryBefore = skill("sorcery");
		charGen.getEarlyAdultGenerator().getSkillController().select(Shadowrun6Core.getItem(SR6Skill.class, "sorcery"));
		int strengthBefore = attr(ShadowrunAttribute.STRENGTH);
		charGen.getEarlyAdultGenerator().selectAttribute(ShadowrunAttribute.STRENGTH);
		assertEquals(strengthBefore+5, attr(ShadowrunAttribute.STRENGTH));
		assertEquals("Focus skill +4", sorceryBefore+4, skill("sorcery"));
		assertTrue(charGen.getEarlyAdultGenerator().getQualityController().select(quality("analytical_mind")).wasSuccessful());
		Contact mentor = charGen.getEarlyAdultGenerator().createContact(Shadowrun6Core.getItem(ContactType.class, "MAGIC"));
		mentor.setName("Old Mentor");
		assertTrue(charGen.getEarlyAdultGenerator().setContactInfluence(3));
		model.setLifepathComingOfAgeBackground("Discovered magic at sixteen");
		charGen.runProcessors();

		/*
		 * Adult life: 8 modules
		 */
		Map<ShadowrunAttribute,Integer> attrBefore = new HashMap<>();
		for (ShadowrunAttribute key : ShadowrunAttribute.primaryAndSpecialValues())
			attrBefore.put(key, attr(key));
		Map<String,Integer> skillBefore = new HashMap<>();
		for (String id : List.of("perception","con","astral","athletics","influence","sorcery"))
			skillBefore.put(id, skill(id));
		List<String> knowledgeBefore = knowledgeSkills();
		int karmaBeforeModules = model.getKarmaFree();

		selectModule("agent",
				"41d9d69d-6d3a-483d-98a4-243103671dc3=LOGIC",
				"82c68a8e-e879-4a44-9c54-56735b531885=WILLPOWER",
				"47e4b4de-7023-4214-8d3c-7152afaa4fb0=perception",
				"84cc83dc-7b58-45d1-811a-85ddc900fae1=7dd367d3-df22-4ed9-805b-78f6250ae482");  // Spionagetechniken
		selectModule("bodyguard",
				"b1b85bed-d2e6-4215-9391-e78f214c963e=BODY",
				"06b16ee6-384a-4b27-b30c-7d2e400d2634=REACTION",
				"8f92b461-b473-4f08-b5d3-bc5b1d9aff84=perception");
		selectModule("angestellte_bei_stuffer_shack",
				"1976914d-adf5-431d-baae-d0088bb496fe=INTUITION",
				"1963d15f-9e7b-40e8-a6c5-2691c9a18d65=con",
				"eb1a4f8d-682c-4cb9-8ab2-9589103fb11c=3dc48f62-3035-49f4-b8ec-8a3fb2a96609");  // +25000 Nuyen
		selectModule("arbeiterin",
				"3461db82-382b-4daf-961b-c734dddb46fa=WILLPOWER",
				"528b8d3a-8cf8-4fb9-8e97-1abe5180cf3b=INTUITION",
				"9689a16c-ca8a-4a4c-ab32-7c7fee93b768=perception",
				"d2f3f6dd-d4c7-471d-adcd-fa110ae342ef=850437da-e016-490e-975f-5c674c8d43d1");  // Trideoserien
		selectModule("artefaktjaeger",
				"536caf58-7ad4-41e2-bc34-51e1810cf519=LOGIC",
				"74ee0be9-dbee-43cf-9794-162d8afaa8c5=MAGIC",
				"5c7fd703-e66e-40fd-bb69-eb1153348535=astral",
				"e4fbd319-5ad6-4ab2-bdb4-af087c08b06d=814b65e0-3db6-4ec1-a11b-a6367e41d4a3");  // Arkana
		selectModule("athletin",
				"98984cc2-f187-42f3-80ec-828ae8a4f9bf=BODY",
				"2293831a-9502-4c56-a75f-7156fa169db6=AGILITY",
				"5c183eaf-163e-4b55-a11a-9b1b334243e6=athletics",
				"8280696b-2bfc-4beb-a46a-b511857ce92f=afb6324e-ab84-4602-aee5-0bc740d8cb7c");  // Stadtkrieg
		selectModule("barista_slash_barkeeper",
				"6ac7e347-682a-4513-8a7e-c7588381e4d2=CHARISMA",
				"d3d04132-75e3-45a7-a40d-e287fad189dd=con",
				"c73fd585-c595-44df-bd23-53222811230c=d0f00926-6ae7-4a38-b43e-d6609a1368bc");  // Aktuelles Tagesgeschehen
		selectModule("bedienstete",
				"2a0b802a-c3e8-412b-a4a9-da7eff9b4bfb=WILLPOWER",
				"0ef5c2d4-c815-47ba-9c3c-d06beb144535=influence",
				"894e9434-8a43-4da1-b324-99adc542b4c1=2772029d-8b07-48c8-8add-41734f2b03ff",   // MAGIC +1
				"afc86844-8f09-42bd-98e4-5f74a13c88a2=b0e1d524-1dd6-477e-8862-ab98d5596ee2");  // Moderne Kueche
		model.setLifepathAdultBackground("Worked for a corporation, then went underground");

		charGen.finish();

		// Every life stage is complete
		assertNoStopper("Born this way", charGen.getBornThisWayGenerator().getToDos());
		assertNoStopper("Childhood", charGen.getChildhoodGenerator().getToDos());
		assertNoStopper("Coming of age", charGen.getEarlyAdultGenerator().getToDos());
		assertNoStopper("Adult life", charGen.getModuleGenerator().getToDos());

		// Modules don't cost Karma
		assertEquals(karmaBeforeModules, model.getKarmaFree());

		// Attributes: +1 for every chosen attribute, MAGIC also from a <selmod>
		Map<ShadowrunAttribute,Integer> attrDelta = Map.of(
				ShadowrunAttribute.LOGIC, 2, ShadowrunAttribute.WILLPOWER, 3, ShadowrunAttribute.BODY, 2,
				ShadowrunAttribute.REACTION, 1, ShadowrunAttribute.INTUITION, 2, ShadowrunAttribute.MAGIC, 2,
				ShadowrunAttribute.AGILITY, 1, ShadowrunAttribute.CHARISMA, 1);
		for (ShadowrunAttribute key : ShadowrunAttribute.primaryAndSpecialValues()) {
			int expected = attrBefore.get(key) + attrDelta.getOrDefault(key, 0);
			assertEquals("Attribute "+key, expected, attr(key));
		}

		// Skills: +1 for every chosen skill
		Map<String,Integer> skillDelta = Map.of("perception", 3, "con", 2, "astral", 1, "athletics", 1, "influence", 1, "sorcery", 0);
		for (String id : skillDelta.keySet())
			assertEquals("Skill "+id, skillBefore.get(id)+skillDelta.get(id), skill(id));

		// Knowledge skills: the childhood area plus the chosen <selmod> options
		List<String> knowledgeAdded = new ArrayList<>(knowledgeSkills());
		knowledgeBefore.forEach(knowledgeAdded::remove);
		assertTrue("Childhood area missing in "+knowledgeBefore, knowledgeBefore.contains("Hamburg"));
		assertEquals(List.of("Aktuelles Tagesgeschehen", "Arkana", "Moderne Küche", "Spionagetechniken", "Stadtkrieg", "Trideoserien"),
				knowledgeAdded.stream().sorted().toList());

		// Qualities of all life stages
		for (String id : List.of("sinner", "charismatic_defense", "allergy", "analytical_mind"))
			assertTrue("Quality "+id+" missing", model.hasQuality(id));

		// Nuyen: 25000 when coming of age, the modules and the <selmod> of the Stuffer Shack
		assertEquals(25000 + 25000 + 25000 + 25000+25000 + 0 + 25000 + 25000 + 50000 + 25000, model.getNuyen());

		// Contacts: the connection from coming of age and the points of all stages (4 + modules) minus its 4 points
		assertEquals(1, model.getContacts().size());
		assertSame(mentor, model.getContacts().get(0));
		assertEquals(3, mentor.getRating());
		assertEquals(1, mentor.getLoyalty());
		assertEquals(4 + 2+4+4+2+2+2+2+2 - 4, ((SR6ContactGenerator)charGen.getContactController()).getPointsLeft());

		// Everything survives saving and loading
		Shadowrun6Character loaded = Shadowrun6Core.decode(Shadowrun6Core.encode(model));
		assertEquals("Gaius", loaded.getName());
		assertEquals("Born in Hamburg", loaded.getLifepathBabyBackground());
		assertEquals("Grew up on the streets of Hamburg", loaded.getLifepathChildhoodBackground());
		assertEquals("Discovered magic at sixteen", loaded.getLifepathComingOfAgeBackground());
		assertEquals("Worked for a corporation, then went underground", loaded.getLifepathAdultBackground());
		assertEquals(1, loaded.getContacts().size());
		assertEquals(mentor.getId(), loaded.getContacts().get(0).getId());
		assertEquals("Old Mentor", loaded.getContacts().get(0).getName());
		assertEquals(model.getNuyen(), loaded.getNuyen());
		assertEquals(model.getKarmaFree(), loaded.getKarmaFree());
		for (ShadowrunAttribute key : ShadowrunAttribute.primaryAndSpecialValues())
			assertEquals("Loaded attribute "+key, attr(key), loaded.getAttribute(key).getModifiedValue());
	}
}
