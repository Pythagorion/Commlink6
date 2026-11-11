package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.chargen.RecommendationState;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IQualityController;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6RejectReasons;
import de.rpgframework.shadowrun6.chargen.gen.CommonQualityGenerator;

/**
 *
 */
public class OneOrTwoQualityController extends CommonQualityGenerator implements IQualityController {

	private Supplier<Quality> quality1Supplier;
	private Consumer<Quality> quality1Consumer;
	private Supplier<Quality> quality2Supplier;
	private Consumer<Quality> quality2Consumer;
	private Predicate<Quality> quality1Predicate;
	private Supplier<List<Quality>> qualitiesSupplier;
	private Consumer<Quality> qualityAddConsumer;
	private Consumer<Quality> qualityRemoveConsumer;
	private Supplier<Integer> maximumSupplier;
	/** If two qualities are chosen, one must be positive and the other negative */
	private boolean mixedPolarity = true;

	//-------------------------------------------------------------------
	public OneOrTwoQualityController(SR6LifepathCharacterGenerator parent,
			Supplier<Quality> quality1Supplier, Consumer<Quality> quality1Consumer,
			Supplier<Quality> quality2Supplier, Consumer<Quality> quality2Consumer) {
		super(parent);
		this.quality1Supplier = quality1Supplier;
		this.quality1Consumer = quality1Consumer;
		this.quality2Supplier = quality2Supplier;
		this.quality2Consumer = quality2Consumer;
	}

	//-------------------------------------------------------------------
	public OneOrTwoQualityController(SR6LifepathCharacterGenerator parent,
			Supplier<List<Quality>> qualitiesSupplier, Consumer<Quality> qualityAddConsumer,
			Consumer<Quality> qualityRemoveConsumer, Supplier<Integer> maximumSupplier) {
		super(parent);
		this.qualitiesSupplier = qualitiesSupplier;
		this.qualityAddConsumer = qualityAddConsumer;
		this.qualityRemoveConsumer = qualityRemoveConsumer;
		this.maximumSupplier = maximumSupplier;
	}

	//-------------------------------------------------------------------
	/**
	 * @param mixed If TRUE (default), two qualities must be one positive and one negative
	 */
	public OneOrTwoQualityController setMixedPolarity(boolean mixed) {
		this.mixedPolarity = mixed;
		return this;
	}

	//-------------------------------------------------------------------
	private SR6LifePathSettings getSettings() {
		return ((SR6LifepathCharacterGenerator) parent).getSettings();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValueController#getValue(de.rpgframework.genericrpg.NumericalValue)
	 */
	@Override
	public int getValue(QualityValue value) {
		return value.getDistributed();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValueController#canBeIncreased(de.rpgframework.genericrpg.NumericalValue)
	 */
	@Override
	public Possible canBeIncreased(QualityValue value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValueController#canBeDecreased(de.rpgframework.genericrpg.NumericalValue)
	 */
	@Override
	public Possible canBeDecreased(QualityValue value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValueController#increase(de.rpgframework.genericrpg.NumericalValue)
	 */
	@Override
	public OperationResult<QualityValue> increase(QualityValue value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValueController#decrease(de.rpgframework.genericrpg.NumericalValue)
	 */
	@Override
	public OperationResult<QualityValue> decrease(QualityValue value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getAvailable()
	 */
	@Override
	public List<Quality> getAvailable() {
		if (qualitiesSupplier!=null) {
			List<Quality> selected = getConfiguredQualities();
			int maximum = getMaximumQualities();
			if (selected.size()>=maximum)
				return List.of();
			if (mixedPolarity && maximum<=2 && selected.size()==1) {
				Quality first = selected.get(0);
				return super.getAvailable().stream()
						.filter(p -> p.isPositive() == !first.isPositive())
						.collect(Collectors.toList());
			}
			return super.getAvailable();
		}
		Quality q1 = quality1Supplier.get();
		Quality q2 = quality2Supplier.get();
		if (q1==null && q2==null)
			return super.getAvailable();
		if (q1!=null && q2!=null)
			return List.of();
		if (!mixedPolarity)
			return super.getAvailable();
		if (q1!=null)
			return super.getAvailable().stream()
					// If you choose two qualities, one must be a positive quality and the other must be a negative quality
					.filter(p -> p.isPositive() == !q1.isPositive())
					.collect(Collectors.toList());
		else
			return super.getAvailable().stream()
					// If you choose two qualities, one must be a positive quality and the other must be a negative quality
					.filter(p -> p.isPositive() == !q2.isPositive())
					.collect(Collectors.toList());
//		if (handleFirstQuality) {
//			return super.getAvailable();
//		} else {
//			if (getSettings().getBornQuality1()==null) return List.of();
//			return super.getAvailable().stream()
//					// If you choose two qualities, one must be a positive quality and the other must be a negative quality
//					.filter(p -> p.isPositive() == !getSettings().getBornQuality1().isPositive())
//					.collect(Collectors.toList());
//		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getSelected()
	 */
	@Override
	public List<QualityValue> getSelected() {
		if (qualitiesSupplier!=null) {
			List<String> selected = getConfiguredQualities().stream().map(Quality::getId).toList();
			return super.getSelected().stream().filter(q -> selected.contains(q.getKey())).toList();
		}
		List<String> selected = new ArrayList<>();
		if (quality1Supplier.get()!=null) selected.add(quality1Supplier.get().getId());
		if (quality2Supplier.get()!=null) selected.add(quality2Supplier.get().getId());
		return super.getSelected().stream().filter(q -> selected.contains(q.getKey())).toList();
//		Quality q1 = quality1Supplier.get();
//		Quality q2 = quality2Supplier.get();
//		if (handleFirstQuality) {
//			if (getSettings().getBornQuality1()!=null) {
//				return List.of(parent.getModel().getQuality(getSettings().getBornQuality1().getId()));
//			} else {
//				return List.of();
//			}
//		} else {
//			if (getSettings().getBornQuality2()!=null) {
//				return List.of(parent.getModel().getQuality(getSettings().getBornQuality2().getId()));
//			} else {
//				return List.of();
//			}
//		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public RecommendationState getRecommendationState(Quality value) {
		return RecommendationState.NEUTRAL;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getRecommendationState(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public RecommendationState getRecommendationState(QualityValue value) {
		return RecommendationState.NEUTRAL;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#getChoicesToDecide(de.rpgframework.genericrpg.data.DataItem)
	 */
	@Override
	public List<Choice> getChoicesToDecide(Quality value) {
		return value.getChoices();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeSelected(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public Possible canBeSelected(Quality value, Decision... decisions) {
		int maximumQualities = getMaximumQualities();
		List<Quality> chosen = getChosenQualities();
		if (chosen.size()>=maximumQualities)
			return new Possible(ToDoElement.Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_QUALITY_ALREADY_6, maximumQualities);
		// If you choose two qualities, one must be a positive quality and the other must be a negative quality
		if (mixedPolarity && maximumQualities<=2 && chosen.size()==1 && chosen.get(0).isPositive()==value.isPositive())
			return new Possible(ToDoElement.Severity.STOPPER, SR6RejectReasons.RES, SR6RejectReasons.IMPOSS_LIFEPATH_QUALITY_POLARITY);
		return super.canBeSelected(value, decisions);
	}

	//-------------------------------------------------------------------
	/**
	 * @return TRUE, if two qualities are both positive or both negative
	 */
	public static boolean hasSamePolarity(List<? extends Quality> qualities) {
		return qualities.size()==2 && qualities.get(0).isPositive()==qualities.get(1).isPositive();
	}

	//-------------------------------------------------------------------
	private List<Quality> getChosenQualities() {
		if (qualitiesSupplier!=null)
			return getConfiguredQualities();
		List<Quality> ret = new ArrayList<>();
		if (quality1Supplier.get()!=null) ret.add(quality1Supplier.get());
		if (quality2Supplier.get()!=null) ret.add(quality2Supplier.get());
		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#select(de.rpgframework.genericrpg.data.DataItem, de.rpgframework.genericrpg.data.Decision[])
	 */
	@Override
	public OperationResult<QualityValue> select(Quality quality, Decision... decisions) {
		Possible poss = canBeSelected(quality, decisions);
		if (!poss.get()) {
			if (poss.getMostSevereExcept(List.of(SR6RejectReasons.IMPOSS_ALREADY_PRESENT))!=null) {
				logger.log(Level.WARNING, "Trying to select({0}) but not possible because {1}", quality, poss.getMostSevere());
				return new OperationResult<QualityValue>(poss);
			}
		}

		QualityValue ret = new QualityValue(quality, 1);
		ret.setInjectedBy(this);
		for (Decision dec : decisions) ret.addDecision(dec);

		if (qualitiesSupplier!=null) {
			qualityAddConsumer.accept(quality);
		} else {
			Quality q1 = quality1Supplier.get();
			if (q1==null) {
				quality1Consumer.accept(quality);
			} else {
				quality2Consumer.accept(quality);
			}
		}

		if (parent.getModel().hasQuality(quality.getId())) {
			ret = parent.getModel().getQuality(quality.getId());
			ret.setDistributed(1);
			logger.log(Level.ERROR, "Added +1 to quality : {0}\n", ret);
		} else {
			ret.setDistributed(1);
			parent.getModel().addQuality(ret);
		}
		logger.log(Level.ERROR, "Select quality : {0}\n", quality.getId());

		logger.log(Level.ERROR, "Call runProcessor on "+parent);
		parent.runProcessors();
		return new OperationResult<QualityValue>(ret);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#canBeDeselected(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public Possible canBeDeselected(QualityValue value) {
		if (qualitiesSupplier!=null) {
			boolean selected = getConfiguredQualities().stream().anyMatch(quality -> quality.getId().equals(value.getKey()));
			return selected?Possible.TRUE:Possible.FALSE;
		}
		Quality q1 = quality1Supplier.get();
		Quality q2 = quality2Supplier.get();
		logger.log(Level.WARNING, "canBeDeselected: {0} while q1={1} and q2={2}\n", value.getKey(), q1, q2);
		if (value.getResolved()==q1 || value.getResolved()==q2) return Possible.TRUE;
		return Possible.FALSE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ComplexDataItemController#deselect(de.rpgframework.genericrpg.data.DataItemValue)
	 */
	@Override
	public boolean deselect(QualityValue value) {
		Possible poss = canBeDeselected(value);
		if (!poss.get()) {
			logger.log(Level.WARNING, "Trying to deselect({0}) but not possible because {1}", value.getKey(), poss.getMostSevere());
			return false;
		}
		if (qualitiesSupplier!=null) {
			value.setDistributed(0);
			super.deselect(value);
			qualityRemoveConsumer.accept((Quality)value.getResolved());
			parent.runProcessors();
			return true;
		}
		Quality q1 = quality1Supplier.get();
		Quality q2 = quality2Supplier.get();
		if (value.getResolved()==q1) {
			value.setDistributed(0);
			super.deselect(value);
			quality1Consumer.accept(null);
			parent.runProcessors();
			return true;
		} else if (value.getResolved()==q2) {
			value.setDistributed(0);
			super.deselect(value);
			quality2Consumer.accept(null);
			parent.runProcessors();
			return true;
		} else {
			logger.log(Level.WARNING, "Cannot deselect what isn't selected: "+model.getQualities());
		}

		return false;
	}

	//-------------------------------------------------------------------
	private List<Quality> getConfiguredQualities() {
		if (qualitiesSupplier==null)
			return List.of();
		List<Quality> selected = qualitiesSupplier.get();
		if (selected==null)
			return List.of();
		return selected;
	}

	//-------------------------------------------------------------------
	private int getMaximumQualities() {
		if (maximumSupplier==null)
			return 2;
		return Math.max(0, maximumSupplier.get());
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.PartialController#getToDos()
	 */
	@Override
	public List<ToDoElement> getToDos() {
		return List.of();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.PartialController#roll()
	 */
	@Override
	public void roll() {
		// TODO Auto-generated method stub

	}

//	//-------------------------------------------------------------------
//	/**
//	 * @see de.rpgframework.genericrpg.chargen.PartialController#getChoiceUUIDs()
//	 */
//	@Override
//	public List<UUID> getChoiceUUIDs() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see de.rpgframework.genericrpg.chargen.PartialController#decide(java.lang.Object, java.util.UUID, de.rpgframework.genericrpg.data.Decision)
//	 */
//	@Override
//	public void decide(Quality decideFor, UUID choice, Decision decision) {
//		// TODO Auto-generated method stub
//
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
//	 */
//	@Override
//	public List<Modification> process(List<Modification> unprocessed) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
