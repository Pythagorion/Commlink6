package de.rpgframework.shadowrun6.chargen.lvl;

import java.lang.System.Logger.Level;
import java.util.List;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.Possible.State;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.chargen.RecommendationState;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.GenericRPGTools;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.shadowrun.Focus;
import de.rpgframework.shadowrun.FocusValue;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.ShadowrunRules;
import de.rpgframework.shadowrun.chargen.charctrl.IFocusController;
import de.rpgframework.shadowrun.chargen.charctrl.IRejectReasons;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.ControllerImpl;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.items.ItemTemplate;

/**
 * @author stefa
 *
 */
public class SR6CommonFocusController extends ControllerImpl<Focus> implements IFocusController {

	protected int forcePool;

	//-------------------------------------------------------------------
	/**
	 * @param parent
	 */
	public SR6CommonFocusController(SR6CharacterController parent) {
		super(parent);
		logger = System.getLogger(SR6CommonFocusController.class.getPackageName());
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getAvailable()
	 */
	@Override
	public List<Focus> getAvailable() {
		return Shadowrun6Core.getItemList(Focus.class);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelected()
	 */
	@Override
	public List<FocusValue> getSelected() {
		return getModel().getFoci();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public RecommendationState getRecommendationState(Focus value) {
		return RecommendationState.NEUTRAL;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public RecommendationState getRecommendationState(FocusValue value) {
		return RecommendationState.NEUTRAL;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getChoicesToDecide(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public List<Choice> getChoicesToDecide(Focus value) {
		return value.getChoices();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeSelected(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public Possible canBeSelected(Focus value, Decision... decisions) {
		// Ensure all decisions are present
		Possible poss = GenericRPGTools.areAllDecisionsPresent(value, decisions);
		if (!poss.get() || poss.getState()==State.DECISIONS_MISSING)
			return poss;

		Shadowrun6Character model = getModel();

		// Some foci are restricted to certain magic types
		poss = Shadowrun6Tools.areRequirementsMet(model, value, decisions);
		if (!poss.get())
			return poss;

		/*
		 * General rules: (CRB 154)
		 * - You can’t bond more foci than your Magic attribute,
		 * - no focus Force may exceed your magic Attribute (new in Seattle Edition)
		 * - and the maximum Force of all your bonded foci
		 *   can’t exceed your Magic x 5.
		 */

		int magic = model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue(ValueType.NATURAL);
		logger.log(Level.WARNING, "Magic = {0}   count={1}", magic, model.getFoci().size());
		// Can't bond more foci than MAG
		if (model.getFoci().size()>=magic) {
			// No. foci larger than magic attribute
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_TOO_MANY_FOCI);
		}

		// Determine rating
		Decision decForce = GenericRPGTools.getDecision(ItemTemplate.UUID_RATING, decisions);
		int force = (decForce!=null)?Integer.parseInt(decForce.getValue()):1;
		logger.log(Level.WARNING, "Force = {0}  ", force);
		// No focus may exceed MAG attribute
		if (force>magic) {
			// No. foci larger than magic attribute
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_FOCUS_EXCEEDS_MAGIC, force, magic);
		}

		// Would force pool be exceeded?
		int newForcePool = forcePool - force;
		if (newForcePool<0) {
			// Sum of force of all foci exceeds Magic*5
			return new Possible(Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_SUM_FORCE_EXCEEDS_MAX, newForcePool, magic*5);
		}

		// Enough Karma for bonding
		if ( (force*value.getBondingMultiplier())>model.getKarmaFree()) {
			return new Possible(Severity.STOPPER, IRejectReasons.RES, IRejectReasons.IMPOSS_NOT_ENOUGH_KARMA, force*value.getBondingMultiplier());
		}

		// Enough Nuyen for bonding
		boolean payGear = parent.getRuleController().getRuleValueAsBoolean(ShadowrunRules.CAREER_PAY_GEAR);
		if ( (force*value.getNuyenCost())>model.getNuyen()) {
			if (!model.isInCareerMode() || (model.isInCareerMode() && payGear))
				return new Possible(Severity.STOPPER, IRejectReasons.RES, IRejectReasons.IMPOSS_NOT_ENOUGH_NUYEN, force*value.getNuyenCost(), model.getNuyen());
		}

		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#select(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public OperationResult<FocusValue> select(Focus value, Decision... decisions) {
		Possible poss = canBeSelected(value, decisions);
		if (!poss.get() || poss.getState()==State.DECISIONS_MISSING) {
			logger.log(Level.ERROR, "Trying to select focus, but "+poss);
			return new OperationResult<>(poss);
		}

		Decision decForce = GenericRPGTools.getDecision(ItemTemplate.UUID_RATING, decisions);
		int force = (decForce!=null)?Integer.parseInt(decForce.getValue()):1;

		FocusValue val = new FocusValue(value, force);
		for (Decision dec : decisions) {
			val.addDecision(dec);
		}
		getModel().addFocus(val);
		logger.log(Level.INFO, "Selected focus "+val);
		
		parent.runProcessors();

		return new OperationResult<FocusValue>(val);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeDeselected(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public Possible canBeDeselected(FocusValue value) {
		if (getModel().getFoci().contains(value)) {
			return Possible.TRUE;
		}
		return new Possible(IRejectReasons.IMPOSS_NOT_PRESENT);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#deselect(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public boolean deselect(FocusValue value) {
		Possible poss = canBeDeselected(value);
		if (!poss.get()) {
			logger.log(Level.ERROR, "Trying to deselect focus, but "+poss);
			return false;
		}

		getModel().removeFocus(value);
		logger.log(Level.INFO,"Deselected focus "+value);

		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelectionCost(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public float getSelectionCost(Focus data, Decision... decisions) {
		// TODO Auto-generated method stub
		return 0;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelectionCostString(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public String getSelectionCostString(Focus data) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> unprocessed) {
		Shadowrun6Character model = getModel();
		try {
			forcePool = model.getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue(ValueType.NATURAL)*5;

//			// Pay karma for selected foci - Nuyen are paid in EquipmentController
			for (FocusValue focus : model.getFoci()) {
//				int karma = focus.getCostKarma();
//				logger.info("Pay "+karma+" Karma for force "+focus.getLevel()+" focus "+focus);
//				model.setKarmaFree(model.getKarmaFree()-karma);
//
				forcePool -= focus.getLevel();
			}
		} finally {
			logger.log(Level.TRACE,"STOP : process() ends with "+unprocessed.size()+" modifications still to process");
		}
		return unprocessed;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IFocusController#getFocusPointsMax()
	 */
	@Override
	public int getFocusPointsMax() {
		return getModel().getAttribute(ShadowrunAttribute.MAGIC).getModifiedValue(ValueType.NATURAL)*5;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IFocusController#getFocusPointsSpent()
	 */
	@Override
	public int getFocusPointsSpent() {
		return getFocusPointsMax() - forcePool;
	}

}
