package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.rpgframework.character.RuleSpecificCharacterObject;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.chargen.CharacterController;
import de.rpgframework.genericrpg.chargen.PartialController;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.IAttribute;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ContactType;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun6.CreatePoints;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.SR6SkillValue;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Rules;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.ChildhoodGenerator.SimpleSkillController;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 *
 */
public class EarlyAdultGenerator implements PartialController<Quality> {

	protected static Logger logger = System.getLogger(EarlyAdultGenerator.class.getPackageName());

	/** Points to distribute on influence and loyalty of the connection */
	private final static int CONTACT_POINTS = 4;
	/** Connection types allowed when coming of age */
	private final static List<String> CONTACT_TYPES = List.of("ACADEMIC","CORPORATE","MAGIC","MATRIX","MEDIA","MEDICAL","CRIMINAL","GOVERNMENT","STREET","ENGINEERING");

	private SR6LifepathCharacterGenerator parent;
	protected List<ToDoElement> todos;
	private List<SR6Skill> availableSkills;

	//-------------------------------------------------------------------
	public EarlyAdultGenerator(SR6LifepathCharacterGenerator parent) {
		this.parent = parent;
		this.todos = new ArrayList<>();

		availableSkills = List.of("athletics","close_combat","con","electronics","influence","outdoors","perception","stealth")
				.stream().map(key -> Shadowrun6Core.getSkill(key)).toList();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.PartialController#getToDos()
	 */
	@Override
	public List<ToDoElement> getToDos() {
		return todos;
	}

	//-------------------------------------------------------------------
	public List<SR6Skill> getAvailableSkills() {
		return Shadowrun6Core.getItemList(SR6Skill.class);
	}

//	//-------------------------------------------------------------------
//	public List<Quality> getAvailable1() {
//		return Shadowrun6Core.getItemList(SR6Quality.class).stream()
//				.filter(p -> parent.showDataItem(p))
//				.filter(p -> p.isFreeSelectable())
//				.collect(Collectors.toList());
//	}
//
//	//-------------------------------------------------------------------
//	public List<Quality> getAvailable2() {
//		if (parent.getSettings().getBornQuality1()==null) return List.of();
//		return Shadowrun6Core.getItemList(SR6Quality.class).stream()
//				.filter(p -> parent.showDataItem(p))
//				.filter(p -> p.isFreeSelectable())
//				.filter(p -> !parent.getModel().hasQuality(p.getId()))
//				// If you choose two qualities, one must be a positive quality and the other must be a negative quality
//				.filter(p -> p.isPositive() == !parent.getSettings().getBornQuality1().isPositive())
//				.collect(Collectors.toList());
//	}
//
//	//-------------------------------------------------------------------
//	public Possible canBeSelected1(Quality value, Decision... decisions) {
//		if (getAvailable1().contains(value)) {
//			return Shadowrun6Tools.checkDecisionsAndRequirements(parent.getModel(), value, decisions);
//		}
//		return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_NOT_AVAILABLE, value.getName());
//	}
//
//	//-------------------------------------------------------------------
//	public Possible canBeSelected2(Quality value, Decision... decisions) {
//		if (getAvailable2().contains(value)) {
//			return Shadowrun6Tools.checkDecisionsAndRequirements(parent.getModel(), value, decisions);
//		}
//		return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_NOT_AVAILABLE, value.getName());
//	}
//
//	//-------------------------------------------------------------------
//	public OperationResult<QualityValue> selectQuality1(Quality quality, Decision... decisions) {
//		Possible poss = canBeSelected1(quality);
//		if (!poss.get()) {
//			logger.log(Level.WARNING, "Trying to select({0}) but not possible because {1}", quality, poss.getMostSevere());
//			return new OperationResult<QualityValue>(poss);
//		}
//
//		QualityValue ret = new QualityValue(quality, 1);
//		ret.setInjectedBy(this);
//		for (Decision dec : decisions) ret.addDecision(dec);
//		parent.getSettings().setBornQuality1(quality);
//
//		if (parent.getModel().hasQuality(quality.getId())) {
//			ret = parent.getModel().getQuality(quality.getId());
//			ret.setDistributed(1);
//		} else {
//			parent.getModel().addQuality(ret);
//		}
//		logger.log(Level.INFO, "Select primary quality {0}\n", quality.getId());
//
//		parent.runProcessors();
//		return new OperationResult<QualityValue>(ret);
//	}
//
//	//-------------------------------------------------------------------
//	public OperationResult<QualityValue> selectQuality2(Quality quality, Decision... decisions) {
//		Possible poss = canBeSelected2(quality);
//		if (!poss.get()) {
//			logger.log(Level.WARNING, "Trying to select({0}) but not possible because {1}", quality, poss.getMostSevere());
//			return new OperationResult<QualityValue>(poss);
//		}
//
//		QualityValue ret = new QualityValue(quality, 0);
//		for (Decision dec : decisions) ret.addDecision(dec);
//
//		parent.getModel().addQuality(ret);
//		parent.getSettings().setBornQuality2(quality);
//		logger.log(Level.INFO, "Select secondary quality {0}", quality.getId());
//
//		parent.runProcessors();
//		return new OperationResult<QualityValue>(ret);
//	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> unprocessed) {
		todos.clear();

		// Skills
		if (parent.getSettings().getEarlyAdultSkill()==null) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_EARLY_SKILL_MISSING));
		} else {
			ValueModification mod = new ValueModification(ShadowrunReference.SKILL, parent.getSettings().getEarlyAdultSkill().getId(), 4, EarlyAdultGenerator.class, ValueType.NATURAL);
			unprocessed.add(mod);
		}
		if (parent.getSettings().getEarlyAdultAttribute()==null) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_EARLY_ATTRIBUTE_MISSING));
		} else {
			ValueModification mod = new ValueModification(ShadowrunReference.ATTRIBUTE, parent.getSettings().getEarlyAdultAttribute().name(), 5, EarlyAdultGenerator.class, ValueType.NATURAL);
			unprocessed.add(mod);
		}

		// Qualities
		SR6Quality quality1 = parent.getSettings().getEarlyAdultQuality1();
		SR6Quality quality2 = parent.getSettings().getEarlyAdultQuality2();
		if (quality1==null && quality2==null) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_EARLY_QUALITY_MISSING));
		}
		for (SR6Quality quality : new SR6Quality[] {quality1, quality2}) {
			if (quality!=null && !getModel().hasQuality(quality.getId())) {
				logger.log(Level.INFO, "Coming of age quality: {0}", quality);
				getModel().addQuality(new QualityValue(quality, 1));
			}
		}

		int startNuyen = parent.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_START_NUYEN);
		unprocessed.add(new ValueModification(ShadowrunReference.CREATION_POINTS, CreatePoints.NUYEN.name(), startNuyen));
		// Points for the connection from coming of age - it is paid with them like any other contact
		unprocessed.add(new ValueModification(ShadowrunReference.CREATION_POINTS, CreatePoints.CONTACT_POINTS.name(), CONTACT_POINTS));
		Contact contact = getContact();
		if (contact==null) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_EARLY_CONTACT_MISSING));
		} else if (contact.getRating()<1 || contact.getLoyalty()<1 || contact.getRating()+contact.getLoyalty()!=CONTACT_POINTS) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_EARLY_CONTACT_POINTS));
		}
		return unprocessed;
	}

	@Override
	public <A extends IAttribute, M extends RuleSpecificCharacterObject<A, ?, ?, ?>> CharacterController<A, M> getCharacterController() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shadowrun6Character getModel() {
		return parent.getModel();
	}

	@Override
	public void roll() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<UUID> getChoiceUUIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void decide(Quality decideFor, UUID choice, Decision decision) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	public void selectChildhoodArea(String value) {
		parent.getSettings().setChildhoodArea(value);
		parent.runProcessors();
	}

	//-------------------------------------------------------------------
	/**
	 * One or two qualities in any combination - other than in childhood, both may also be positive or both negative
	 */
	public OneOrTwoQualityController getQualityController() {
		return new OneOrTwoQualityController(parent,
				() -> parent.getSettings().getEarlyAdultQuality1(),
				 q -> parent.getSettings().setEarlyAdultQuality1((SR6Quality) q),
				() -> parent.getSettings().getEarlyAdultQuality2(),
				 q -> parent.getSettings().setEarlyAdultQuality2((SR6Quality) q)
				 ).setMixedPolarity(false);
	}

	//-------------------------------------------------------------------
	/**
	 * The connection can be of any of these types
	 */
	public List<ContactType> getAvailableContactTypes() {
		return CONTACT_TYPES.stream()
				.map(id -> Shadowrun6Core.getItem(ContactType.class, id))
				.filter(type -> type!=null)
				.toList();
	}

	//-------------------------------------------------------------------
	/**
	 * @return The connection the character has known for a very long time, or NULL
	 */
	public Contact getContact() {
		UUID id = parent.getSettings().getEarlyAdultContact();
		if (id==null)
			return null;
		return getModel().getContacts().stream()
				.filter(con -> id.equals(con.getId()))
				.findFirst().orElse(null);
	}

	//-------------------------------------------------------------------
	/**
	 * Create the connection with 2 points each on influence and loyalty.
	 * If it already exists, it is returned unchanged.
	 */
	public Contact createContact(ContactType type) {
		Contact contact = getContact();
		if (contact!=null)
			return contact;
		if (!getAvailableContactTypes().contains(type))
			return null;
		contact = new Contact();
		contact.setType(type);
		contact.setRating(2);
		contact.setLoyalty(CONTACT_POINTS-2);
		getModel().addContact(contact);
		parent.getSettings().setEarlyAdultContact(contact.getId());
		parent.runProcessors();
		return contact;
	}

	//-------------------------------------------------------------------
	public boolean setContactType(ContactType type) {
		Contact contact = getContact();
		if (contact==null || !getAvailableContactTypes().contains(type))
			return false;
		contact.setType(type);
		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * Distribute the 4 points: influence as given, the rest goes to loyalty
	 */
	public boolean setContactInfluence(int influence) {
		Contact contact = getContact();
		if (contact==null || influence<1 || influence>CONTACT_POINTS-1)
			return false;
		contact.setRating(influence);
		contact.setLoyalty(CONTACT_POINTS-influence);
		parent.runProcessors();
		return true;
	}

	public SimpleSkillController getSkillController() {
		return new SimpleSkillController() {

			@Override
			public boolean canBeSelected(SR6Skill skill) {
				return true;
			}

			@Override
			public boolean isSelected(SR6Skill skill) {
				return parent.getSettings().getEarlyAdultSkill()==skill;
			}

			@Override
			public void select(SR6Skill skill) {
				parent.getSettings().setEarlyAdultSkill(skill);
				logger.log(Level.WARNING, "Selected early adult skill {0}", skill);
				parent.runProcessors();
			}

			@Override
			public void deselect(SR6Skill skill) {
				parent.getSettings().setEarlyAdultSkill(null);
				logger.log(Level.INFO, "Deselected early adult skill {0}", skill);
				parent.runProcessors();
			}
		};
	}

	//-------------------------------------------------------------------
	public List<ShadowrunAttribute> getAvailableAttributes() {
		return List.of(ShadowrunAttribute.primaryValues());
	}

	//-------------------------------------------------------------------
	public void selectAttribute(ShadowrunAttribute attribute) {
		parent.getSettings().setEarlyAdultAttribute(attribute);
		parent.runProcessors();
	}

	//-------------------------------------------------------------------
	public void deselectAttribute(ShadowrunAttribute attribute) {
		if (parent.getSettings().getEarlyAdultAttribute()==attribute) {
			parent.getSettings().setEarlyAdultAttribute(null);
			parent.runProcessors();
		}
	}
}


