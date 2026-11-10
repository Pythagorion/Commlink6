package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.rpgframework.character.RuleSpecificCharacterObject;
import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.chargen.CharacterController;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.chargen.PartialController;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.IAttribute;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Rules;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 *
 */
public class BornThisWayGenerator implements PartialController<Quality> {

	protected static Logger logger = System.getLogger(BornThisWayGenerator.class.getPackageName());

	private SR6LifepathCharacterGenerator parent;
	protected List<ToDoElement> todos;

	//-------------------------------------------------------------------
	public BornThisWayGenerator(SR6LifepathCharacterGenerator parent) {
		this.parent = parent;
		this.todos = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.PartialController#getToDos()
	 */
	@Override
	public List<ToDoElement> getToDos() {
		return todos;
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

		if (parent.getSettings().getBornQualities().isEmpty()) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_BORN_QUALITY_MISSING));
		} else {
			for (SR6Quality quality : parent.getSettings().getBornQualities()) {
				logger.log(Level.INFO, "Born this way Quality: {0}", quality);
				addBornQuality(quality);
			}
			int maximum = getMaximumBornQualities();
			if (parent.getSettings().getBornQualities().size()>maximum)
				todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_QUALITY_TOO_MANY, maximum));
			if (OneOrTwoQualityController.hasSamePolarity(parent.getSettings().getBornQualities()))
				todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_LIFEPATH_QUALITY_POLARITY));
		}
		for (QualityValue quality : parent.getSettings().getSurgeQualities()) {
			addSurgeQuality(quality);
		}
		applySurgeKarma();

		if (parent.getSettings().getNativeLanguage()==null) {
			todos.add( new ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_LANGUAGE_NOT_SET));
		} else {
			logger.log(Level.INFO, "Native language: {0}", parent.getSettings().getNativeLanguage());
			SR6Skill lang = Shadowrun6Core.getSkill("language");
			ValueModification mod = new ValueModification(ShadowrunReference.SKILL, "language", 4, BornThisWayGenerator.class, ValueType.NATURAL);
			UUID uuid = lang.getChoices().get(0).getUUID();
			mod.getDecisions().add(new Decision(uuid, parent.getSettings().getNativeLanguage()));
			unprocessed.add(mod);
		}
		return unprocessed;
	}

	//-------------------------------------------------------------------
	private void addBornQuality(Quality quality) {
		if (!getModel().hasQuality(quality.getId())) {
			getModel().addQuality(new QualityValue(quality, 1));
		}
	}

	//-------------------------------------------------------------------
	private void addSurgeQuality(QualityValue value) {
		if (value==null || value.getResolved()==null)
			return;
		if (!getModel().hasQuality(value.getKey())) {
			QualityValue copy = new QualityValue(value.getResolved(), value.getDistributed());
			copy.setInjectedBy(this);
			value.getDecisions().forEach(copy::addDecision);
			getModel().addQuality(copy);
		}
	}

	//-------------------------------------------------------------------
	private void applySurgeKarma() {
		int surgeKarma = parent.getSettings().getSurgeQualities().stream()
				.mapToInt(SR6LifePathSurgeQualityController::getSurgeKarmaCost)
				.sum();
		if (surgeKarma!=0) {
			logger.log(Level.INFO, "Apply Life Path SURGE Karma {0}", surgeKarma);
			getModel().setKarmaFree(getModel().getKarmaFree() - surgeKarma);
		}
	}

	@Override
	public <A extends IAttribute, M extends RuleSpecificCharacterObject<A, ?, ?, ?>> CharacterController<A, M> getCharacterController() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shadowrun6Character getModel() {
		return  parent.getModel();
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
	public void selectNativeLanguage(String value) {
		parent.getSettings().setNativeLanguage(value);
		parent.runProcessors();
	}

	//-------------------------------------------------------------------
	/**
	 * One or two qualities - but never more than the overall life path limit allows
	 */
	public int getMaximumBornQualities() {
		int maximumQualities = parent.getRuleController().getRuleValueAsInteger(Shadowrun6Rules.CHARGEN_LIFEPATH_MAX_QUALITIES);
		return Math.min(2, Math.max(0, maximumQualities - 2));
	}

	//-------------------------------------------------------------------
	public OneOrTwoQualityController getQualityController() {
		return new OneOrTwoQualityController(parent,
				() -> new ArrayList<Quality>(parent.getSettings().getBornQualities()),
				 q -> parent.getSettings().addBornQuality((SR6Quality) q),
				 q -> parent.getSettings().removeBornQuality((SR6Quality) q),
				() -> getMaximumBornQualities()
				 );
	}

//	//-------------------------------------------------------------------
//	public OneOrTwoQualityController getQualityController1() {
//		return new OneOrTwoQualityController(parent, true,
//				() -> parent.getSettings().getBornQuality1(),
//				 q -> parent.getSettings().setBornQuality1(q),
//				() -> parent.getSettings().getBornQuality2(),
//				 q -> parent.getSettings().setBornQuality2(q)
//				 );
//	}
//
//	//-------------------------------------------------------------------
//	public OneOrTwoQualityController getQualityController2() {
//		return new OneOrTwoQualityController(parent, false,
//				() -> parent.getSettings().getBornQuality1(),
//				 q -> parent.getSettings().setBornQuality1(q),
//				() -> parent.getSettings().getBornQuality2(),
//				 q -> parent.getSettings().setBornQuality2(q)
//				 );
//	}
}


