package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.chargen.ComplexDataItemController;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.chargen.RecommendationState;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.GenericRPGTools;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.genericrpg.requirements.AnyRequirement;
import de.rpgframework.genericrpg.requirements.ExistenceRequirement;
import de.rpgframework.genericrpg.requirements.Requirement;
import de.rpgframework.shadowrun6.CreatePoints;
import de.rpgframework.shadowrun6.LifepathModule;
import de.rpgframework.shadowrun6.LifepathModuleValue;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.ControllerImpl;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 *
 */
public class SR6LifePathModuleGenerator extends ControllerImpl<LifepathModule>
		implements ComplexDataItemController<LifepathModule, LifepathModuleValue> {

	private int selectionsLeft;

	//-------------------------------------------------------------------
	/**
	 * @param parent
	 */
	public SR6LifePathModuleGenerator(SR6LifepathCharacterGenerator parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getAvailable()
	 */
	@Override
	public List<LifepathModule> getAvailable() {
		return Shadowrun6Core.getItemList(LifepathModule.class).stream()
			.filter(m -> parent.showDataItem(m))
			.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelected()
	 */
	@Override
	public List<LifepathModuleValue> getSelected() {
		return ((SR6LifepathCharacterGenerator) parent).getSettings().getModules();
	}

	//-------------------------------------------------------------------
	public int getMaximumModules() {
		return ((SR6LifepathCharacterGenerator) parent).getLifePathMaximumModules();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public RecommendationState getRecommendationState(LifepathModule item) {
		if (item==null) return RecommendationState.NEUTRAL;
		// No recommender set, means no recommendation
		if (parent.getRecommender().isEmpty()) {
			return RecommendationState.NEUTRAL;
		}

		return parent.getRecommender().get().getRecommendationState(item);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public RecommendationState getRecommendationState(LifepathModuleValue value) {
		return getRecommendationState(value.getResolved());
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getChoicesToDecide(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public List<Choice> getChoicesToDecide(LifepathModule value) {
		if (value==null)
			return List.of();
		return value.getChoices();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeSelected(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public Possible canBeSelected(LifepathModule value, Decision... decisions) {
		if (value==null) return Possible.FALSE;
		int maximumModules = getMaximumModules();
		if (getSelected().size()>=maximumModules)
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_LIFEPATH_MODULE_LIMIT, maximumModules);
		long sameModule = getSelected().stream()
				.filter(val -> val.getKey().equals(value.getId()))
				.count();
		if (sameModule>=2)
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_ALREADY_PRESENT, value.getName());

		Possible qualityChoices = validateQualityChoices(value, decisions);
		if (!qualityChoices.get())
			return qualityChoices;

		Possible selmodChoices = validateSelmodChoices(value, decisions);
		if (!selmodChoices.get())
			return selmodChoices;

		Possible requirements = Shadowrun6Tools.checkDecisionsAndRequirements(getModel(), value, decisions);
		if (!requirements.get())
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_REQUIREMENTS_NOT_MET);
		return requirements;
	}

	//-------------------------------------------------------------------
	private Possible validateQualityChoices(LifepathModule value, Decision... decisions) {
		for (Choice choice : value.getChoices()) {
			if (choice.getChooseFrom()!=ShadowrunReference.QUALITY || choice.getTypeReference()==null)
				continue;
			Decision decision = findDecision(choice, decisions);
			if (decision==null || decision.getValue()==null || decision.getValue().isBlank())
				continue;
			SR6Quality quality = Shadowrun6Core.getItem(SR6Quality.class, decision.getValue());
			if (quality==null)
				return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_REQUIREMENTS_NOT_MET);
			if (!matchesQualityChoiceReference(quality, choice.getTypeReference()))
				return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_REQUIREMENTS_NOT_MET);
		}
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * Every &lt;selmod&gt; must be decided by the ID of one of its options
	 */
	private Possible validateSelmodChoices(LifepathModule value, Decision... decisions) {
		for (Modification mod : value.getOutgoingModifications()) {
			if (!(mod instanceof ModificationChoice selMod))
				continue;
			Decision decision = findDecision(selMod.getUUID(), decisions);
			boolean validOption = decision!=null && selMod.getModificiations().stream()
					.anyMatch(opt -> opt instanceof DataItemModification dataMod && dataMod.getId()!=null
							&& dataMod.getId().toString().equals(decision.getValue()));
			if (!validOption)
				return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_LIFEPATH_SELMOD_MISSING);
		}
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	private Decision findDecision(Choice choice, Decision... decisions) {
		return findDecision(choice.getUUID(), decisions);
	}

	//-------------------------------------------------------------------
	private Decision findDecision(UUID choiceUUID, Decision... decisions) {
		for (Decision decision : decisions) {
			if (decision!=null && choiceUUID.equals(decision.getChoiceUUID()))
				return decision;
		}
		return null;
	}

	//-------------------------------------------------------------------
	private boolean matchesQualityChoiceReference(SR6Quality quality, String reference) {
		if ("POSITIVE".equalsIgnoreCase(reference))
			return quality.isPositive();
		if ("NEGATIVE".equalsIgnoreCase(reference))
			return !quality.isPositive();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#select(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public OperationResult<LifepathModuleValue> select(LifepathModule value, Decision... decisions) {
		logger.log(Level.DEBUG, "ENTER select({0})", value);
		try {
			Possible poss = canBeSelected(value, decisions);
			if (poss.getState()!=Possible.State.POSSIBLE) {
				logger.log(Level.WARNING, "Trying to select({0}) but not possible because {1}", value, poss.getMostSevere());
				return new OperationResult<>(poss);
			}

			LifepathModuleValue val = new LifepathModuleValue(value);
			for (Decision dec : decisions) {
				val.addDecision(dec);
			}
			((SR6LifepathCharacterGenerator) parent).getSettings().addModule(val);
			logger.log(Level.INFO, "Selected lifepath {1} module:{0}", value.getId(), value.getType());

			parent.runProcessors();
			return new OperationResult<LifepathModuleValue>(val);
		} finally {
			logger.log(Level.DEBUG, "LEAVE select({0})", value);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeDeselected(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public Possible canBeDeselected(LifepathModuleValue value) {
		if (!((SR6LifepathCharacterGenerator) parent).getSettings().getModules().contains(value)) {
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_NOT_PRESENT, value.getNameWithoutRating());
		}
		List<LifepathModuleValue> dependents = getModulesDependingOn(value);
		if (!dependents.isEmpty()) {
			String names = dependents.stream()
					.map(module -> module.getResolved().getName(parent.getLocale()))
					.collect(Collectors.joining(", "));
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_LIFEPATH_MODULE_REQUIRED, names);
		}
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	private List<LifepathModuleValue> getModulesDependingOn(LifepathModuleValue prerequisite) {
		List<LifepathModuleValue> ret = new ArrayList<>();
		for (LifepathModuleValue selected : getSelected()) {
			if (selected==prerequisite)
				continue;
			if (wouldLoseRequirement(selected, prerequisite))
				ret.add(selected);
		}
		return ret;
	}

	//-------------------------------------------------------------------
	private boolean wouldLoseRequirement(LifepathModuleValue dependent, LifepathModuleValue removed) {
		LifepathModule module = dependent.getResolved();
		for (Requirement req : module.getRequirements()) {
			boolean metBefore = isRequirementMetWithSelection(req, module, dependent.getDecisionArray(), null);
			boolean metAfter = isRequirementMetWithSelection(req, module, dependent.getDecisionArray(), removed);
			if (metBefore && !metAfter)
				return true;
		}
		return false;
	}

	//-------------------------------------------------------------------
	private boolean isRequirementMetWithSelection(Requirement req, LifepathModule requiredFor, Decision[] decisions, LifepathModuleValue removed) {
		if (req instanceof ExistenceRequirement) {
			ExistenceRequirement tmp = (ExistenceRequirement)req;
			if (tmp.getType()==ShadowrunReference.LIFEMOD) {
				boolean hasModule = hasSelectedModule(req.getKey(), removed);
				return tmp.isNegate() ? !hasModule : hasModule;
			}
		}
		if (req instanceof AnyRequirement) {
			AnyRequirement any = (AnyRequirement)req;
			for (Requirement option : any.getOptionList()) {
				if (isRequirementMetWithSelection(option, requiredFor, decisions, removed))
					return true;
			}
			return false;
		}
		return Shadowrun6Tools.isRequirementMet(getModel(), requiredFor, req, decisions);
	}

	//-------------------------------------------------------------------
	private boolean hasSelectedModule(String key, LifepathModuleValue removed) {
		for (LifepathModuleValue selected : getSelected()) {
			if (selected==removed)
				continue;
			if (selected.getKey().equals(key))
				return true;
		}
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#deselect(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public boolean deselect(LifepathModuleValue value) {
		Possible poss = canBeDeselected(value);
		if (!poss.get()) {
			logger.log(Level.WARNING, "Trying to deselect({0}) but not possible because {1}", value.getKey(), poss.getMostSevere());
			return false;
		}
		((SR6LifepathCharacterGenerator) parent).getSettings().removeModule(value);
		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelectionCost(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public float getSelectionCost(LifepathModule data, Decision... decisions) {
		return 1;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		selectionsLeft = 0;
		todos.clear();
		List<Modification> unprocessed = new ArrayList<>();
		try {
			for (Modification tmp : previous) {
				switch ((ShadowrunReference)tmp.getReferenceType()) {
				case CREATION_POINTS:
					ValueModification valMod = (ValueModification)tmp;
					CreatePoints what = valMod.getResolvedKey();
					switch (what) {
					case LIFEPATH_MODULES:
						selectionsLeft += valMod.getValue();
						break;
					default:
						unprocessed.add(valMod);
					}
					break;
				default:
					unprocessed.add(tmp);
				}
			}
			logger.log(Level.DEBUG, "I can select {0} lifepath modules", selectionsLeft);

			// Now inject modifications from modules
			for (LifepathModuleValue modVal : getSelected()) {
				selectionsLeft--;
				LifepathModule module = modVal.getResolved();
				for (Modification tmp : module.getOutgoingModifications()) {
					if (tmp instanceof ModificationChoice selMod) {
						// Only the chosen option of a <selmod> applies
						List<Modification> chosen = GenericRPGTools.resolveModificationChoice(selMod, modVal);
						if (chosen==null) {
							todos.add(new de.rpgframework.genericrpg.ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_LIFEPATH_SELMOD_MISSING, module.getName()));
							continue;
						}
						chosen.forEach(mod -> addModuleModification(localizeDecisions(mod, module), module, modVal, unprocessed));
					} else {
						addModuleModification(tmp, module, modVal, unprocessed);
					}
				}

			}
			int maximumModules = getMaximumModules();
			if (getSelected().size()<maximumModules) {
				todos.add(new de.rpgframework.genericrpg.ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_NOT_ENOUGH_LIFEPATH_MODULES, maximumModules - getSelected().size()));
			} else if (getSelected().size()>maximumModules) {
				todos.add(new de.rpgframework.genericrpg.ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_TOO_MANY_LIFEPATH_MODULES, getSelected().size() - maximumModules));
			}
			Map<String, Long> counts = getSelected().stream()
					.map(LifepathModuleValue::getKey)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			for (Map.Entry<String, Long> entry : counts.entrySet()) {
				if (entry.getValue()>2) {
					todos.add(new de.rpgframework.genericrpg.ToDoElement(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.TODO_TOO_MANY_LIFEPATH_MODULES, entry.getKey()));
				}
			}

		} finally {

		}

		return unprocessed;
	}

	//-------------------------------------------------------------------
	/**
	 * Replace "i18n.&lt;text&gt;" decisions with the localized text, falling back to the text itself.
	 * Works on a copy, so the module data stays untouched.
	 */
	private Modification localizeDecisions(Modification mod, LifepathModule module) {
		if (!(mod instanceof DataItemModification dataMod)
				|| dataMod.getDecisions().stream().noneMatch(dec -> dec.getValue()!=null && dec.getValue().startsWith("i18n.")))
			return mod;

		List<Decision> localized = new ArrayList<>();
		for (Decision dec : dataMod.getDecisions()) {
			if (dec.getValue()!=null && dec.getValue().startsWith("i18n.")) {
				String text = dec.getValue().substring(5);
				String key  = "lifemod."+module.getId()+"."+text;
				String result = module.getLocalizedString(Locale.getDefault(), key);
				if (result==null || result.isBlank() || result.contains(key))
					result = text;
				localized.add(new Decision(dec.getChoiceUUID(), result));
			} else {
				localized.add(dec);
			}
		}
		DataItemModification ret = dataMod.clone();
		ret.setDecisions(localized);
		ret.setSource(module);
		return ret;
	}

	//-------------------------------------------------------------------
	private void addModuleModification(Modification tmp, LifepathModule module, LifepathModuleValue modVal, List<Modification> unprocessed) {
		if (tmp instanceof DataItemModification mod && mod.getConnectedChoice()!=null) {
			Choice choice = module.getChoice(mod.getConnectedChoice());
			Decision dec  = modVal.getDecision(choice.getUUID());
			List<Modification> toAdd = GenericRPGTools.decisionToModifications(mod, choice, dec);
			logger.log(Level.INFO, "For {0} decision {1} for {2} leads to {3}",modVal.getKey(), dec.getValue(), dec.getChoiceUUID(), toAdd);
			toAdd.forEach(generated -> unprocessed.add(normalizeModuleModification(generated, module)));
		} else {
			unprocessed.add(normalizeModuleModification(tmp, module));
		}
	}

	//-------------------------------------------------------------------
	private Modification normalizeModuleModification(Modification mod, LifepathModule module) {
		if (mod instanceof ValueModification && mod.getReferenceType()==ShadowrunReference.SKILL) {
			ValueModification val = (ValueModification) mod;
			if (val.getSet()!=ValueType.NATURAL) {
				ValueModification ret = new ValueModification(ShadowrunReference.SKILL, val.getKey(), val.getValue(), module, ValueType.NATURAL);
				ret.getDecisions().addAll(val.getDecisions());
				return ret;
			}
		}
		return mod;
	}

}
