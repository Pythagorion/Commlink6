package de.rpgframework.shadowrun6.chargen.jfx.selector;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prelle.javafx.CloseType;
import org.prelle.javafx.FlexibleApplication;
import org.prelle.javafx.JavaFXConstants;
import org.prelle.javafx.ManagedDialog;
import org.prelle.javafx.NavigButtonControl;
import org.prelle.javafx.OptionalNodePane;

import de.rpgframework.HasName;
import de.rpgframework.ResourceI18N;
import de.rpgframework.core.BabylonEventBus;
import de.rpgframework.core.BabylonEventType;
import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.chargen.CharacterController;
import de.rpgframework.genericrpg.chargen.ChoiceCallback;
import de.rpgframework.genericrpg.chargen.ComplexDataItemController;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.ChoiceOption;
import de.rpgframework.genericrpg.data.ComplexDataItem;
import de.rpgframework.genericrpg.data.ComplexDataItemValue;
import de.rpgframework.genericrpg.data.DataItem;
import de.rpgframework.genericrpg.data.DataItemValue;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.IReferenceResolver;
import de.rpgframework.genericrpg.data.SkillSpecialization;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.items.PieceOfGear;
import de.rpgframework.genericrpg.items.Usage;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.shadowrun.ASpell;
import de.rpgframework.shadowrun.AdeptPower;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.DamageElement;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.MentorSpirit;
import de.rpgframework.shadowrun.NPCType;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.ShadowrunElement;
import de.rpgframework.shadowrun.chargen.charctrl.IFocusController;
import de.rpgframework.shadowrun.items.AugmentationQuality;
import de.rpgframework.shadowrun6.SR6NPC;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6RuleFlag;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.SR6Spell;
import de.rpgframework.shadowrun6.Sense;
import de.rpgframework.shadowrun6.Shadowrun6Action;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.SignatureManeuver;
import de.rpgframework.shadowrun6.chargen.charctrl.ISR6EquipmentController;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.gen.CommonQualityGenerator;
import de.rpgframework.shadowrun6.chargen.jfx.pane.CarriedItemDescriptionPane;
import de.rpgframework.shadowrun6.chargen.jfx.pane.FocusValueDescriptionPane;
import de.rpgframework.shadowrun6.items.AmmunitionType;
import de.rpgframework.shadowrun6.items.ItemHook;
import de.rpgframework.shadowrun6.items.ItemSubType;
import de.rpgframework.shadowrun6.items.ItemTemplate;
import de.rpgframework.shadowrun6.items.ItemType;
import de.rpgframework.shadowrun6.items.ItemTypeFilter;
import de.rpgframework.shadowrun6.items.SR6GearTool;
import de.rpgframework.shadowrun6.items.SR6ItemAttribute;
import de.rpgframework.shadowrun6.items.SR6ItemFlag;
import de.rpgframework.shadowrun6.items.SR6PieceOfGearVariant;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * @author prelle
 *
 */
public class ChoiceSelectorDialog<T extends ComplexDataItem, V extends ComplexDataItemValue<T> > extends ManagedDialog implements ChoiceCallback<T> {

	private final static PropertyResourceBundle RES = (PropertyResourceBundle) ResourceBundle.getBundle(ChoiceSelectorDialog.class.getPackageName()+".Selectors");

	private final static Logger logger = System.getLogger(ChoiceSelectorDialog.class.getPackageName());

	private final static Choice VARIANT_CHOICE = new Choice(PieceOfGear.VARIANT, null);
	private final static Choice RATING_CHOICE = new Choice(ItemTemplate.UUID_RATING, null);

	private ComplexDataItemController<T,V> ctrl;
	/* Only relevant for ItemTemplates */
	private CarryMode carry;

	private OptionalNodePane optional;
	private GenericDescriptionVBox bxDesc;
	private VBox content;
	private Label lbProblem;
	private NavigButtonControl btnCtrl;

	private T item;
	private ItemHook hook;
	private DataItemValue context;
	private Shadowrun6Character model;
	private SR6PieceOfGearVariant selectedVariant;
	private Integer selectedRating;
	private List<Choice> choices;
	private Map<SR6PieceOfGearVariant, List<Node>> perVariantChoices = new HashMap<>();
	private Map<Choice, Decision> decisions = new LinkedHashMap<>();
	private List<SR6ItemFlag> selectedFlasgs = new ArrayList<>();

	private List<Node> toDeleteOnMentorSpirit = new ArrayList<>();
	/** Selection boxes for <selmod>s - a visible one must be decided */
	private List<ChoiceBox<Modification>> selmodBoxes = new ArrayList<>();
	/** Choice to reflect the decision of the player to use the magician or adept advantages */
	private Choice magicianOrAdept = new Choice(CommonQualityGenerator.MENTOR_SPIRIT_ADVANTAGES, ShadowrunReference.MAGIC_RESO);
	private BooleanProperty chooseAdeptAdvantages = new SimpleBooleanProperty(false);
	private BooleanProperty useBothAdvantages = new SimpleBooleanProperty(false);

	private SR6PieceOfGearVariant suggestedVariant;
	private boolean canBeLeftWithOK;

	//-------------------------------------------------------------------
	public ChoiceSelectorDialog(ComplexDataItemController<T,V> ctrl) {
		this(ctrl, null);
	}

	//-------------------------------------------------------------------
	public ChoiceSelectorDialog(ComplexDataItemController<T,V> ctrl, CarryMode carry) {
		this(ctrl, carry, null, null);
	}

	//-------------------------------------------------------------------
	public ChoiceSelectorDialog(ComplexDataItemController<T,V> ctrl, CarryMode carry, ItemHook hook, DataItemValue context) {
		super("Select",null, CloseType.CANCEL, CloseType.OK);
		this.ctrl = ctrl;
		this.carry = carry;
		this.context = context;
		this.hook  = hook;

		content = new VBox(10);
		CharacterController<ShadowrunAttribute,Shadowrun6Character> charCtrl = ctrl.getCharacterController();
		if (ctrl instanceof ISR6EquipmentController) {
			logger.log(Level.DEBUG, "Use special info pane for CarriedItem");
			bxDesc = new CarriedItemDescriptionPane(r -> Shadowrun6Tools.getRequirementString(r, Locale.getDefault()), (SR6CharacterController)charCtrl );
		} else if (ctrl instanceof IFocusController) {
			logger.log(Level.DEBUG, "Use special info pane for FocusValue");
			bxDesc = new FocusValueDescriptionPane(r -> Shadowrun6Tools.getRequirementString(r, Locale.getDefault()), (SR6CharacterController)charCtrl );
		} else {
			logger.log(Level.DEBUG, "Use generic description pane");
			bxDesc = new GenericDescriptionVBox(
					Shadowrun6Tools.requirementResolver(Locale.getDefault()),
					Shadowrun6Tools.modificationResolver(Locale.getDefault()));
		}
		optional= new OptionalNodePane(content, bxDesc);
		lbProblem = new Label();
		lbProblem.setStyle("-fx-text-fill: -fx-accent");

		setContent(new VBox(10,optional, lbProblem));

		Shadowrun6Character model = ctrl.getModel();
		chooseAdeptAdvantages.setValue( model.getMagicOrResonanceType()!=null && model.getMagicOrResonanceType().usesPowers());
		useBothAdvantages.set(model.hasRuleFlag(SR6RuleFlag.MENTOR_SPIRIT_BOTH));
	}

	//-------------------------------------------------------------------
	public void setModel(Shadowrun6Character model) {
		this.model = model;
	}

	//-------------------------------------------------------------------
	private void showHelpFor(DataItem item) {
		System.err.println("ChoiceSelector: showHelpFor("+item.getId()+") on "+bxDesc);
		bxDesc.setData(item);
		optional.setTitle(item.getName());
	}

	//-------------------------------------------------------------------
	private void showHelpFor(DataItemValue item) {
		bxDesc.setData(item);
		optional.setTitle(item.getNameWithoutRating());
	}

	//-------------------------------------------------------------------
	private Decision[] getDecisions() {
		Decision[] ret = new Decision[decisions.size()];
		if (item.hasLevel() && selectedRating!=null)
			ret = new Decision[decisions.size()+1];
		int i=0;
		for (Entry<Choice, Decision> entry : decisions.entrySet()) {
			ret[i] = entry.getValue();
			logger.log(Level.DEBUG, "Decision [{0}] = {1}", i, entry.getValue());
			i++;
		}
		if (item.hasLevel() && selectedRating!=null)
			ret[i] = new Decision(RATING_CHOICE, String.valueOf(selectedRating));
//		for (int i=0; i<choices.size(); i++) {
//			ret[i] = decisions.get(choices.get(i));
//		}
		return ret;
	}

	//-------------------------------------------------------------------
	public String getSelectedVariant() {
		if (selectedVariant!=null)
			return selectedVariant.getId();
		return null;
	}

	//-------------------------------------------------------------------
	private Possible updateButtonsForItemTemplate() {
		CarriedItem<ItemTemplate> container = (context instanceof CarriedItem)?((CarriedItem<ItemTemplate>)context):null;
		CarriedItem<ItemTemplate> carried = null;
		ISR6EquipmentController control = (ISR6EquipmentController)ctrl;

		// Build item so far as possible
		Shadowrun6Character lifeform = ctrl.getModel();
		OperationResult<CarriedItem<ItemTemplate>> result = SR6GearTool.buildItem( (ItemTemplate)item, carry, selectedVariant, lifeform, true, (IReferenceResolver)context, getDecisions());
		logger.log(Level.DEBUG, "Trying to build {0} returned {1}",carry,result);
		if (result.get()!=null) {
			logger.log(Level.INFO, "item to embed: {0}", result.get());
			carried = result.get();
			if (carried.getCarryMode()==CarryMode.EMBEDDED && container!=null) {
				carried.setParent(container);
			}
			logger.log(Level.DEBUG, "item definition for {0} is {1} ",carry,((ItemTemplate)item).getMainOrVariant(carry));
			logger.log(Level.DEBUG, "item def. class for {0} is {1} ",carry,((ItemTemplate)item).getMainOrVariant(carry).getClass());
			logger.log(Level.DEBUG, "usage of {0} is {1} ",carry,((ItemTemplate)item).getMainOrVariant(carry).getUsage(carry));
			bxDesc.setData(carried);
		} else {
			logger.log(Level.WARNING, "Not successful to build item");
			return Possible.FALSE;
		}

		logger.log(Level.DEBUG, "from item {0}", item);
		logger.log(Level.DEBUG, ".. and variant {0}", selectedVariant);
		logger.log(Level.DEBUG, ".. build carried {0}", carried);


		Possible possible = null;
		if (carry==CarryMode.EMBEDDED) {
			logger.log(Level.DEBUG, ".. and now try to {0} it in {1} of {2}", carry, hook, container);
			logger.log(Level.INFO, "call canBeEmbedded on {0} with {1} in hook {2} ",ctrl.getClass().getSimpleName(),Arrays.toString(getDecisions()), hook);
			String variantID = (selectedVariant!=null)?selectedVariant.getId():null;
			possible = control.canBeEmbedded(container, hook,
						(ItemTemplate)item, variantID, getDecisions() );
			logger.log(Level.INFO, "canBeEmbedded({0}) returns "+possible, carry);
		} else {
			logger.log(Level.INFO, "call canBeSelected on "+ctrl.getClass().getSimpleName()+" with "+Arrays.toString(getDecisions()));
			String variantID = (selectedVariant!=null)?selectedVariant.getId():null;
			possible = control.canBeSelected((ItemTemplate)item, variantID, carry, getDecisions() );
			logger.log(Level.INFO, "canBeSelected({0}) returns "+possible, carry);
		}

		return possible;
	}

	//-------------------------------------------------------------------
	private void updateButtons() {
		logger.log(Level.INFO, "updateButtons");
		Possible possible = null;
		// Special handling for gear
		if (item instanceof ItemTemplate) {
			possible = updateButtonsForItemTemplate();
		} else {
			possible = ctrl.canBeSelected(item, getDecisions() );
		}

		// Set status
		ToDoElement problem = possible.getMostSevere();
		if (problem==null) {
			lbProblem.setText(null);
		} else {
			lbProblem.setText(problem.getMessage(Locale.getDefault()));
			switch (problem.getSeverity()) {
			case STOPPER: lbProblem.setStyle("-fx-text-fill: -fx-accent"); break;
			case WARNING: lbProblem.setStyle("-fx-text-fill: primary"); break;
			case INFO   : lbProblem.setStyle("-fx-text-fill: -fx-text-base-color"); break;
			}
		}

		// Every shown <selmod> must be decided
		boolean selmodMissing = selmodBoxes.stream()
			.anyMatch(cb -> cb.getParent()!=null && cb.isVisible() && cb.getValue()==null);

		logger.log(Level.INFO, "Item selection possible=={0}   selmodMissing={1}   btnCtrl={2}",possible,selmodMissing,btnCtrl);
		if (btnCtrl != null) {
			if (selmodMissing || !possible.getRequireDecisions() || (problem != null && problem.getSeverity() != Severity.INFO)) {
				logger.log(Level.DEBUG, " disable OK");
				canBeLeftWithOK = false;
				btnCtrl.setDisabled(CloseType.OK, true);
				this.buttonDisabledProperty().put(CloseType.OK, true);
			} else {
				logger.log(Level.DEBUG, " enable OK ");
				canBeLeftWithOK = true;
				btnCtrl.setDisabled(CloseType.OK, false);
				this.buttonDisabledProperty().put(CloseType.OK, false);
			}
		} else {
			canBeLeftWithOK = possible.get() && !selmodMissing;
			if (btnCtrl!=null)
				btnCtrl.setDisabled(CloseType.OK, !canBeLeftWithOK);
			this.buttonDisabledProperty().put(CloseType.OK, !canBeLeftWithOK);
		}
	}

	//-------------------------------------------------------------------
	private Label addLabel(String title) {
		Label lbName = new Label(title);
		lbName.getStyleClass().add(JavaFXConstants.STYLE_HEADING5);
		content.getChildren().add(lbName);
		return lbName;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ChoiceCallback#apply(java.lang.Object, java.util.List, java.util.List)
	 */
	@Override
	public Decision[] apply(T item, List<Choice> choices, List<ModificationChoice> selections) {
		logger.log(Level.WARNING, "ENTER apply({0}, {1}, {2}) for container {3}", item, choices, selections, context);
		this.item = item;
		this.choices = choices;
		CloseType closed = null;
		try {
			decisions.clear();
			selmodBoxes.clear();
			// The dialog may be created long before the magic type is known - check again
			Shadowrun6Character model = ctrl.getModel();
			chooseAdeptAdvantages.setValue( model.getMagicOrResonanceType()!=null && model.getMagicOrResonanceType().usesPowers());
			useBothAdvantages.set(model.hasRuleFlag(SR6RuleFlag.MENTOR_SPIRIT_BOTH));

			setTitle(ResourceI18N.format(RES, "title", item.getName()));
			bxDesc.setData(item);
			content.getChildren().clear();
			// Minimal intro text
			Label explain = new Label(ResourceI18N.get(RES, "explain"));
			explain.setWrapText(true);
			content.getChildren().add(explain);

			// Eventually prepare variants
			if ((item instanceof ItemTemplate) && !((ItemTemplate)item).getVariants().isEmpty()) {
				processVariants( (ItemTemplate)item );
			}
			// Eventually prepare hardcoded rating
			if (item.hasLevel() && !(item instanceof Quality)) {
				processRating( item );
			}

			for (Choice choice : choices) {
				logger.log(Level.WARNING, "Found choice {0} and controller {1}", choice, ctrl);
				String forceTitle = null;
				if (choice.getUUID().equals(ItemTemplate.UUID_RATING)) forceTitle=ResourceI18N.get(RES, "label.rating");
				if (choice.getUUID().equals(ItemTemplate.UUID_CHEMICAL_CHOICE)) forceTitle=ResourceI18N.get(RES, "label.chemical");
				if (choice.getI18nKey()!=null) forceTitle=null;
				processChoice(item,choice, forceTitle);
			}
			for (ModificationChoice selection : selections) {
				processModificationChoice(item.getName(), selection);
			}

			if (item instanceof ItemTemplate) {
				for (SR6ItemFlag flag : item.getUserSelectableFlags(SR6ItemFlag.class)) {
					processFlag((ItemTemplate) item, flag);
				}
			}

			btnCtrl = new NavigButtonControl();
			btnCtrl.setCallback( (close) -> isButtonEnabled(close));
			btnCtrl.initialize(FlexibleApplication.getInstance(), this);
//			btnCtrl.setDisabled(CloseType.OK, true);
//			this.buttonDisabledProperty().put(CloseType.OK, true);
			updateButtons();
			logger.log(Level.DEBUG, "showAlertAndCall with btnCtrl="+btnCtrl);
			closed = FlexibleApplication.getInstance().showAlertAndCall(this, btnCtrl);
			logger.log(Level.DEBUG, "Closed with "+closed);
			if (closed==CloseType.CANCEL)
				return null;
			return getDecisions();
		} finally {
			logger.log(Level.INFO, "LEAVE apply({0}, {1} with {2})", item, choices, closed);
		}
	}


	// -------------------------------------------------------------------
	private void processRating(ComplexDataItem template) {
		logger.log(Level.INFO, "hasLevel detected");
		addLabel(ResourceI18N.get(RES, "label.rating"));
		ChoiceBox<Integer> cbRatings = new ChoiceBox<>();
		int max = 10;
		if (template instanceof Quality && ((Quality)template).getMax()>0)
			max = ((Quality)template).getMax();
		for (int i=1; i<=max; i++)
			cbRatings.getItems().add(i);
		cbRatings.getSelectionModel().select(0);
		cbRatings.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "Chose rating {0}", n);
			selectedRating = n;
			if (n==null)
				decisions.remove(RATING_CHOICE);
			else
				decisions.put(RATING_CHOICE, new Decision(ItemTemplate.UUID_RATING, String.valueOf(n)));
			updateButtons();
		 });
		content.getChildren().add(cbRatings);

		updateButtons();
	}

	//-------------------------------------------------------------------
	private boolean isButtonEnabled(CloseType close) {
		if (close==CloseType.CANCEL) return true;
		return true;
	}

	// -------------------------------------------------------------------
	private void processVariants(ItemTemplate template) {
		logger.log(Level.INFO, "variants detected");
		addLabel(ResourceI18N.get(RES, "label.variant"));
		ChoiceBox<SR6PieceOfGearVariant> cbVariants = new ChoiceBox<>();
		cbVariants.setMinWidth(150);
		// If no item is required, add a "regular item"
		List<Usage> mainUsages = template.getUsages().stream().filter(us -> us.getMode()==carry && us.getSlot()==hook).toList();
//		logger.log(Level.INFO, "TODO: template usages "+template.getUsages()+" and carrymode "+carry+" and hook "+hook+" --> main usages "+mainUsages);
		if (!template.requiresVariant() && !mainUsages.isEmpty()) {			
			cbVariants.getItems().add(null);
		}
//		logger.log(Level.INFO, "TODO: Check if variants are suitable for "+context+" and carrymode "+carry);
		List<SR6PieceOfGearVariant> filtered = template.getVariants().stream()
				.filter(var -> carry==null || var.getUsage(carry)!=null || var.getUsages().isEmpty())
				.toList();
		if (filtered.size()!=template.getVariants().size()) {
			logger.log(Level.INFO, "Filtered variants for carry mode {0} down to {1}", carry, filtered);
		}
		cbVariants.getItems().addAll(filtered);
		cbVariants.setConverter(new StringConverter<SR6PieceOfGearVariant>() {
			public SR6PieceOfGearVariant fromString(String value) { return null;}
			public String toString(SR6PieceOfGearVariant value) {
				if (value==null) return ResourceI18N.get(RES, "variant.regular");
				String name = template.getVariantName(value, Locale.getDefault());
				if (name.startsWith(template.getTypeString())) {
					ItemSubType sub = template.getAttribute(SR6ItemAttribute.ITEMSUBTYPE).getValue();
					if (sub==null) {
						name = template.getLocalizedString(Locale.getDefault(), "variant."+value.getId());
					} else {
						name = template.getLocalizedString(Locale.getDefault(), "variant."+sub.name().toLowerCase()+"."+value.getId());
					}
				}
				return name;
			}
		});
		cbVariants.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "Chose variant {0}", n);
			selectedVariant = n;
			if (n==null)
				decisions.remove(VARIANT_CHOICE);
			else
				decisions.put(VARIANT_CHOICE, new Decision(ItemTemplate.VARIANT,n.getId()));
			// Hide old variant nodes
			if (perVariantChoices.containsKey(o)) {
				logger.log(Level.DEBUG, "Hide all UI elements for old variant {0}", o);
				for (Node node : perVariantChoices.get(o)) {
					node.setVisible(false);
					node.setManaged(false);
					if (node instanceof ChoiceBox) ((ChoiceBox)node).getSelectionModel().clearSelection();
				}
			}
			// Show new variant nodes
			if (perVariantChoices.containsKey(n)) {
				logger.log(Level.DEBUG, "Show all UI elements for selected variant {0}", o);
				for (Node node : perVariantChoices.get(n)) {
					node.setVisible(true);
					node.setManaged(true);
				}
			}
			updateButtons();
		 });
		content.getChildren().add(cbVariants);

		// Make a list of choices that only exists in a variant
		for (SR6PieceOfGearVariant variant : template.getVariants()) {
			if (variant.getChoices()!=null && !variant.getChoices().isEmpty()) {
				List<Node> allVariantNodes = new ArrayList<>();
				// Prepare UI components
				for (Choice choice : variant.getChoices()) {
					String forceTitle = null;
					if (choice.getUUID().equals(ItemTemplate.UUID_RATING)) forceTitle=ResourceI18N.get(RES, "label.rating");
					if (choice.getUUID().equals(ItemTemplate.UUID_CHEMICAL_CHOICE)) forceTitle=ResourceI18N.get(RES, "label.chemical");
					List<Node> list = processChoice(item,choice, forceTitle);
					logger.log(Level.INFO, "Variant choice returned "+list);
					allVariantNodes.addAll(list);
				}
				perVariantChoices.put(variant, allVariantNodes);
				// Per default hide all variant nodes
				for (Node node : allVariantNodes) {
					node.setVisible(false);
					node.setManaged(false);
				}
			}
		}

		if (suggestedVariant!=null) {
			selectedVariant = suggestedVariant;
			cbVariants.getSelectionModel().select(suggestedVariant);
		} else {
			cbVariants.getSelectionModel().select(0);
		}
		updateButtons();
	}

	// -------------------------------------------------------------------
	private void processFlag(ItemTemplate item, SR6ItemFlag flag) {
		logger.log(Level.DEBUG, "Flag "+flag);
		System.err.println("ChoiceSelectorDialog: Flag "+flag);
		CheckBox checkBox = new CheckBox(flag.getName());
		checkBox.selectedProperty().addListener( (ov,o,n) -> {
			if (n) {
				logger.log(Level.DEBUG, "Selected flag {0}", n);
				selectedFlasgs.add(flag);
			} else {
				logger.log(Level.DEBUG, "Deselected flag {0} again", n);
				selectedFlasgs.remove(flag);
			}
			updateButtons();
		 });
		content.getChildren().add(checkBox);
	}

	// -------------------------------------------------------------------
	private List<Node>  processChoice(ComplexDataItem item, Choice choice, String forceTitle) {
		logger.log(Level.DEBUG, "Choice " + choice);
		List<Node> ret = new ArrayList<>();

		Label label = addLabel(
				(forceTitle==null)
				?
				item.getChoiceName(choice, Locale.getDefault())
				:
				forceTitle);
		ret.add(label);
		switch ((ShadowrunReference) choice.getChooseFrom()) {
		case ACTION:
//			ret.add( handleGeneric(ShadowrunElement.class, choice, item));
			ret.add( handleACTION(item, choice) );
			break;
		case ADEPT_POWER:
			ret.add( handleGeneric(item, choice, AdeptPower.class, null));
			break;
		case ATTRIBUTE:
			if (choice.getTypeReference()==null) {
				ret.add( handleATTRIBUTE(item, choice));
			} else {
				if (!"CHOICE".equals(choice.getTypeReference()))
					label.setText(ShadowrunAttribute.valueOf(choice.getTypeReference()).getName());
				ret.add( handleATTRIBUTEValues(item, choice));
			}
			break;
		case AMMUNITION_TYPE:
			ret.add( handleAMMUNITION_TYPE(item, choice) );
			break;
		case AUGMENTATION_QUALITY:
			ret.add( handleAUGMENTATIONQUALITY(item, choice));
			break;
		case CARRIED:
			ret.add( handleCARRIED(item, choice));
			break;
		case CONTACT:
			ret.add( handleCONTACT(item, choice));
			break;
		case DAMAGE_ELEMENT:
			ret.add( handleGeneric(DamageElement.class, choice, item));
			break;
		case ELEMENT:
			ret.add( handleGeneric(ShadowrunElement.class, choice, item));
			break;
		case GEAR:
			ret.add( handleGEAR(item, choice));
			break;
		case ITEM_ATTRIBUTE:
			if (choice.getChoiceOptions()!=null) {
				ret.add( handleITEMATTRIBUTEValues(item, choice));
			} else if (choice.getTypeReference()!=null && choice.getMaxFormula()!=null) {
				ret.add( handleITEMATTRIBUTEValues(item, choice, SR6ItemAttribute.valueOf(choice.getTypeReference()) ,choice.getMaxFormula().getAsInteger()));
			} else {
				ret.add( handleITEMATTRIBUTE(item, choice));
			}
			break;
		case MATRIX_ATTRIBUTE:
			ret.add( handleITEMATTRIBUTE(item, choice, SR6ItemAttribute.matrixValues()));
			break;
		case MENTOR_SPIRIT:
			ret.add( handleMENTOR_SPIRIT(item, choice, MentorSpirit.Type.MENTOR_SPIRIT) );
			break;
		case PARAGON:
			ret.add( handleMENTOR_SPIRIT(item, choice, MentorSpirit.Type.PARAGON) );
			break;
		case PROGRAM:
			ret.add( handlePROGRAM(item, choice));
			break;
		case QUALITY:
			ret.add( handleQUALITY(item, choice));
			break;
		case SENSE:
			ret.add( handleSENSE(item, choice) );
			break;
		case SIGNATURE_MANEUVERS:
			ret.add( handleSignatureManeuvers(item, choice));
			break;
		case SKILL:
			ret.add( handleSKILL(item, choice) );
			break;
		case SKILLSPECIALIZATION:
			ret.add( handleSKILLSPECIALIZATION(item, choice) );
			break;
		case SPELL:
			ret.add( handleSPELL(item, choice) );
			break;
		case SPELL_CATEGORY:
			ret.add( handleSPELL_CATEGORY(item, choice) );
			break;
		case SPIRIT:
			ret.add( handleGeneric(item, choice, SR6NPC.class, npc -> npc.getType()==NPCType.SPIRIT));
			break;
		case SPRITE:
			ret.add( handleGeneric(item, choice, SR6NPC.class, npc -> npc.getType()==NPCType.SPRITE));
			break;
		case SUBSELECT:
			ret.add( handleSUBSELECT(item, choice) );
			break;
		case TEXT:
			ret.add( handleTEXT(item, choice) );
			break;
		default:
			System.err.println("Not implemented: choosing from " + choice.getChooseFrom());
			logger.log(Level.ERROR, "Not implemented: choosing from " + choice.getChooseFrom());
		}
		return ret;
	}

	//-------------------------------------------------------------------
	private List<Node> processModificationChoice(String title, ModificationChoice choice) {
		logger.log(Level.WARNING, "ModificationChoice " + choice);
		List<Node> ret = new ArrayList<>();
		ret.add(addLabel(title));

		ChoiceBox<Modification> cbChoices = new ChoiceBox<>();
		cbChoices.setUserData(choice);
		cbChoices.getItems().addAll(choice.getModificiations());
		cbChoices.setConverter(new StringConverter<Modification>() {
			public String toString(Modification mod) {
				if (mod==null) return "-";
				return Shadowrun6Tools.getModificationString(mod, Locale.getDefault());
			}
			public Modification fromString(String string) { return null; }
		});
		// Decision references the selected modification by its ID
		Choice asChoice = new Choice(choice.getUUID(), null);
		cbChoices.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			if (n instanceof DataItemModification) {
				decisions.put(asChoice, new Decision(choice.getUUID(), ((DataItemModification)n).getId()));
			} else {
				decisions.remove(asChoice);
			}
			updateButtons();
		 });
		content.getChildren().add(cbChoices);
		selmodBoxes.add(cbChoices);
		ret.add(cbChoices);
		return ret;
	}

	//-------------------------------------------------------------------
	private Node handleSKILL(ComplexDataItem item, Choice choice) {
		ChoiceBox<SR6Skill> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<SR6Skill>() {
			public SR6Skill fromString(String value) { return null;}
			public String toString(SR6Skill value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
					Shadowrun6Core.getItemList(SR6Skill.class).stream().filter(s -> ids.contains(s.getId())).collect(Collectors.toList())
					);
		} else {
			cbSub.getItems().addAll(Shadowrun6Core.getItemList(SR6Skill.class));
		}
		Collections.sort(cbSub.getItems(), new Comparator<SR6Skill>() {
			public int compare(SR6Skill o1, SR6Skill o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));
			updateButtons();
			showHelpFor(n); });
		content.getChildren().add(cbSub);
		return cbSub;
	}
	//-------------------------------------------------------------------
	private Node handleSKILLSPECIALIZATION(ComplexDataItem item, Choice choice) {
		logger.log(Level.ERROR, "handleSKILLSPECIALIZATION");
		ChoiceBox<SkillSpecialization<SR6Skill>> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<SkillSpecialization<SR6Skill>>() {
			public SkillSpecialization<SR6Skill> fromString(String value) { return null;}
			public String toString(SkillSpecialization<SR6Skill> value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			for (String fullID: ids) {
				String[] split = fullID.split("/");
				SR6Skill skill = Shadowrun6Core.getSkill(split[0]);
				if (skill==null) {
					logger.log(Level.ERROR, "Item {0} references unknown skill {1}", item.getTypeString(), skill.getId());
				} else {
					SkillSpecialization<SR6Skill> spec = skill.getSpecialization(split[1]);
					if (spec!=null) {
						cbSub.getItems().add(spec);
					} else {
						logger.log(Level.ERROR, "Item {0} references unknown specialization {1} in skill {2}", item.getTypeString(), split[1],skill.getId());
					}
				}
			}
		} else if (choice.getTypeReference()!=null) {
			SR6Skill skill = Shadowrun6Core.getSkill(choice.getTypeReference());
			List<SkillSpecialization<?>> list = skill.getSpecializations();
			for (SkillSpecialization<?> spec : list) {
				cbSub.getItems().add((SkillSpecialization<SR6Skill>) spec);
			}
//		} else {
//			cbSub.getItems().addAll(Shadowrun6Core.getItemList(SR6Skill.class));
		}
		Collections.sort(cbSub.getItems(), new Comparator<SkillSpecialization<SR6Skill>>() {
			public int compare(SkillSpecialization<SR6Skill> o1, SkillSpecialization<SR6Skill> o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getSkill().getId()+"/"+n.getId()));
			updateButtons();
			showHelpFor(n); });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleATTRIBUTE(ComplexDataItem item, Choice choice) {
		ChoiceBox<ShadowrunAttribute> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<ShadowrunAttribute>() {
			public ShadowrunAttribute fromString(String value) { return null;}
			public String toString(ShadowrunAttribute value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		logger.log(Level.DEBUG, "handleATTRIBUTE: ref={0}  options={1}", choice.getTypeReference(), (choice.getChoiceOptions()!=null)?Arrays.toString(choice.getChoiceOptions()):"[]");
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
				List.of(ShadowrunAttribute.values()).stream().filter(s -> ids.contains(s.name())).collect(Collectors.toList())
				);
		} else {
			cbSub.getItems().addAll(ShadowrunAttribute.primaryValues());
		}

		Collections.sort(cbSub.getItems(), new Comparator<ShadowrunAttribute>() {
			public int compare(ShadowrunAttribute o1, ShadowrunAttribute o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.name()));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleATTRIBUTEValues(ComplexDataItem item, Choice choice) {
		ChoiceBox<String> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<String>() {
			public String fromString(String value) { return null;}
			public String toString(String value) {
				if (value==null) return "-";
				try {
					return  ShadowrunAttribute.valueOf(value).getName() ;
				} catch (Exception e) {
				}
				return value;
			}
		});
		logger.log(Level.DEBUG, "handleATTRIBUTEValues: ref={0}  options={1}", choice.getTypeReference(), (choice.getChoiceOptions()!=null)?Arrays.toString(choice.getChoiceOptions()):"[]");
		cbSub.getItems().addAll( List.of(choice.getChoiceOptions()) );

		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleITEMATTRIBUTE(ComplexDataItem item, Choice choice) {
		return handleITEMATTRIBUTE(item, choice, SR6ItemAttribute.values());
	}

	//-------------------------------------------------------------------
	private Node handleITEMATTRIBUTE(ComplexDataItem item, Choice choice, SR6ItemAttribute[] selectFrom) {
		ChoiceBox<SR6ItemAttribute> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<SR6ItemAttribute>() {
			public SR6ItemAttribute fromString(String value) { return null;}
			public String toString(SR6ItemAttribute value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
					List.of(SR6ItemAttribute.values()).stream().filter(s -> ids.contains(s.name())).collect(Collectors.toList())
					);
		} else {
			cbSub.getItems().addAll(selectFrom);
		}
		Collections.sort(cbSub.getItems(), new Comparator<SR6ItemAttribute>() {
			public int compare(SR6ItemAttribute o1, SR6ItemAttribute o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.name()));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleITEMATTRIBUTEValues(ComplexDataItem item, Choice choice) {
		ChoiceBox<String> cbSub = new ChoiceBox<>();
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(List.of(choice.getChoiceOptions()));
		} else {
			throw new IllegalArgumentException("Only use this method with choice options");
		}
		// Options may have names, e.g. lifestyles for ratings (COM6-277)
		// <choice ... options="1,2,3" i18n="lifestyle"/>  ->  item.<id>.choice.lifestyle.<option>
		if (choice.getI18nKey()!=null) {
			String prefix = item.getTypeString()+"."+item.getId().toLowerCase()+".choice."+choice.getI18nKey()+".";
			cbSub.setConverter(new StringConverter<String>() {
				public String fromString(String value) { return null;}
				public String toString(String value) {
					if (value==null) return "-";
					String name = item.getLocalizedString(Locale.getDefault(), prefix+value);
					// Missing keys return the item ID
					return (name==null || name.equals(item.getId()) || name.equals(prefix+value)) ? value : name;
				}
			});
		}
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleITEMATTRIBUTEValues(ComplexDataItem item, Choice choice, SR6ItemAttribute attrib, int max) {
		ChoiceBox<String> cbSub = new ChoiceBox<>();
		for (int i=1; i<=max; i++) {
			cbSub.getItems().add(i+"");
		}
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1} in {2]", n, attrib, choice.getUUID());
			decisions.put(choice, new Decision(choice, n));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleAUGMENTATIONQUALITY(ComplexDataItem item, Choice choice) {
		ChoiceBox<AugmentationQuality> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<AugmentationQuality>() {
			public AugmentationQuality fromString(String value) { return null;}
			public String toString(AugmentationQuality value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
					List.of(AugmentationQuality.values()).stream().filter(s -> ids.contains(s.name())).collect(Collectors.toList())
					);
		} else {
			cbSub.getItems().addAll(AugmentationQuality.values());
		}
		// STANDARD first, the rest alphabetically
		Collections.sort(cbSub.getItems(), new Comparator<AugmentationQuality>() {
			public int compare(AugmentationQuality o1, AugmentationQuality o2) {
				if (o1==o2) return 0;
				if (o1==AugmentationQuality.STANDARD) return -1;
				if (o2==AugmentationQuality.STANDARD) return 1;
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			if (n!=null) {
				decisions.put(choice, new Decision(choice, n.name()));
			} else {
				decisions.remove(choice);
			}
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		// Most augmentations are bought in standard quality (COM6-149)
		if (cbSub.getItems().contains(AugmentationQuality.STANDARD))
			cbSub.getSelectionModel().select(AugmentationQuality.STANDARD);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private void populateAdeptChoices(MentorSpirit n) {
		for (Modification tmp: n.getAdeptModifications()) {
			if (tmp instanceof ModificationChoice) {
				ModificationChoice mc = (ModificationChoice)tmp;
				List<Node> added = processModificationChoice(ResourceI18N.get(RES,"choice.adept"), mc);
				logger.log(Level.ERROR, "Added for modification choice: "+added);
				added.forEach(node -> {
					node.visibleProperty().bind(chooseAdeptAdvantages.or(useBothAdvantages));
					node.managedProperty().bind(chooseAdeptAdvantages.or(useBothAdvantages));
				});
			} else {
				DataItemModification mod = (DataItemModification)tmp;
				Object val = mod.getResolvedKey();
				if (val instanceof ComplexDataItem) {
					ComplexDataItem cplx = (ComplexDataItem) val;
					for (Choice choice : cplx.getChoices()) {
						logger.log(Level.WARNING, "Found mentor adept choice {0}", choice);
						Choice cloned = (Choice) choice.clone();
						if (mod.getChoiceOptions()!=null && mod.getConnectedChoice()!=null && mod.getConnectedChoice().equals(choice.getUUID())) {
							logger.log(Level.DEBUG, "Restrict choice options from {0} to {1}", Arrays.toString(cloned.getChoiceOptions()), mod.getChoiceOptions());
							cloned.setChoiceOptions(mod.getChoiceOptions());
						}
						List<Node> added = processChoice(cplx, cloned, cplx.getName());
						added.forEach(node -> {
							node.visibleProperty().bind(chooseAdeptAdvantages.or(useBothAdvantages));
							node.managedProperty().bind(chooseAdeptAdvantages.or(useBothAdvantages));
						});
					}
				}
			}
		}
	}

	//-------------------------------------------------------------------
	private void populateMagicianChoices(MentorSpirit n) {
		for (Modification tmp: n.getMagicianModifications()) {
			if (tmp instanceof ModificationChoice) {
				ModificationChoice mc = (ModificationChoice)tmp;
				List<Node> added = processModificationChoice(ResourceI18N.get(RES,"choice.magician"), mc);
				logger.log(Level.ERROR, "Added for modification choice: "+added);
				added.forEach(node -> {
					node.visibleProperty().bind(chooseAdeptAdvantages.not().or(useBothAdvantages));
					node.managedProperty().bind(chooseAdeptAdvantages.not().or(useBothAdvantages));
				});
			} else {
				DataItemModification mod = (DataItemModification)tmp;
				Object val = mod.getResolvedKey();
				if (val instanceof ComplexDataItem) {
					ComplexDataItem cplx = (ComplexDataItem) val;
					for (Choice choice : cplx.getChoices()) {
						logger.log(Level.WARNING, "Found mentor spirit magician choice {0}", choice);
						List<Node> added = processChoice(cplx, choice, cplx.getName());
						added.forEach(node -> {
							node.visibleProperty().bind(chooseAdeptAdvantages.not().or(useBothAdvantages));
							node.managedProperty().bind(chooseAdeptAdvantages.not().or(useBothAdvantages));
						});
				}
				}
			}
		}
	}

	//-------------------------------------------------------------------
	private <T extends DataItem> Node handleGeneric(ComplexDataItem item, Choice choice, Class<T> cls, Predicate<T> filter) {
		ChoiceBox<T> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<T>() {
			public T fromString(String value) { return null;}
			public String toString(T value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		Collection<T> list = Shadowrun6Core.getItemList(cls);
		if (filter!=null) {
			list = list.stream().filter(filter).collect(Collectors.toList());
		}
		choicebox.getItems().addAll(list);
		Collections.sort(choicebox.getItems(), new Comparator<T>() {
			public int compare(T o1, T o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private <T extends HasName> Node handleGeneric(Class<T> item, Choice choice, ComplexDataItem parent) {
		ChoiceBox<T> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<T>() {
			public T fromString(String value) { return null;}
			public String toString(T value) {
				if (value==null) return "-";
				// If there is a per choice translation, use it instead of the common translation
				ChoiceOption opt = new ChoiceOption();
				opt.setId(value.getId());
				String key = parent.getChoiceOptionString(choice, value, Locale.getDefault());
				if (!key.contains("choice"))
					return key;
				return value.getName(Locale.getDefault());
			}
		});
		if (choice.getChoiceOptions()!=null) {
			List<String> valid = List.of(choice.getChoiceOptions());
			for (T opt : item.getEnumConstants()) {
				if (valid.contains(opt.getId().toUpperCase())) {
					choicebox.getItems().add(opt);
				}
			}
		} else {
			choicebox.getItems().addAll(item.getEnumConstants());
		}
		Collections.sort(choicebox.getItems(), new Comparator<T>() {
			public int compare(T o1, T o2) {
				return Collator.getInstance().compare(o1.getName(Locale.getDefault()), o2.getName(Locale.getDefault()));
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, ((Enum<?>)n).name()));
			updateButtons();
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleAMMUNITION_TYPE(ComplexDataItem item, Choice choice) {
		ChoiceBox<AmmunitionType> cbAmmoType = new ChoiceBox<>();
		cbAmmoType.setConverter(new StringConverter<AmmunitionType>() {
			public AmmunitionType fromString(String value) { return null;}
			public String toString(AmmunitionType value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		cbAmmoType.getItems().addAll(Shadowrun6Core.getItemList(AmmunitionType.class));
		Collections.sort(cbAmmoType.getItems(), new Comparator<AmmunitionType>() {
			public int compare(AmmunitionType o1, AmmunitionType o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbAmmoType.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(cbAmmoType);

		return cbAmmoType;
	}

	//-------------------------------------------------------------------
	private Node handleMENTOR_SPIRIT(ComplexDataItem item, Choice choice, MentorSpirit.Type type) {
		logger.log(Level.WARNING, "handleMENTOR_SPIRIT");
		ChoiceBox<MentorSpirit> cbMentor = new ChoiceBox<>();
		cbMentor.setConverter(new StringConverter<MentorSpirit>() {
			public MentorSpirit fromString(String value) { return null;}
			public String toString(MentorSpirit value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbMentor.getItems().addAll(
					Shadowrun6Core.getItemList(MentorSpirit.class).stream()
						.filter(s -> ctrl.getCharacterController().showDataItem(s))
						.filter(s -> ids.contains(s.getId()))
						.filter(s -> s.getType()==type)
						.filter(Shadowrun6Tools.filterByLanguage(Locale.getDefault()))
						.collect(Collectors.toList())
					);
		} else {
			cbMentor.getItems().addAll(
					Shadowrun6Core.getItemList(MentorSpirit.class).stream()
					.filter(Shadowrun6Tools.filterByLanguage(Locale.getDefault()))
					.filter(s -> s.getType()==type)
					.collect(Collectors.toList())
				);
		}
		Collections.sort(cbMentor.getItems(), new Comparator<MentorSpirit>() {
			public int compare(MentorSpirit o1, MentorSpirit o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbMentor.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.WARNING, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			// Clear content from previous mentor spirit selection
			if (o!=null && !o.getChoices().isEmpty()) {
				choices.removeAll(o.getChoices());
				o.getChoices().forEach(c -> decisions.remove(c));
			}
			if (o!=null) {
				// Forget decisions for <selmod>s of the previous mentor spirit
				List<UUID> selmods = Stream.concat(o.getMagicianModifications().stream(), o.getAdeptModifications().stream())
					.filter(m -> m instanceof ModificationChoice)
					.map(m -> ((ModificationChoice)m).getUUID())
					.toList();
				decisions.keySet().removeIf(c -> selmods.contains(c.getUUID()));
			}
			content.getChildren().removeAll(toDeleteOnMentorSpirit);

			// Memorize current GUI elements
			List<Node> current = new ArrayList<Node>(content.getChildren());

			// Populate with generic decisions
			logger.log(Level.INFO, "Choices of Mentor spirit: "+n.getChoices());
			for (Choice tmpChoice : n.getChoices()) {
				logger.log(Level.WARNING, "Found generic mentor spirit choice {0}", choice);
				processChoice(item, tmpChoice, null);
				choices.add(choice);
			}
			populateMagicianChoices(n);
			populateAdeptChoices(n);

			// Compare with now updated
			List<Node> updated = new ArrayList<Node>(content.getChildren());
			toDeleteOnMentorSpirit  = updated.stream().filter(i -> !current.contains(i)).collect(Collectors.toList());


			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(cbMentor);

		Shadowrun6Character model = ctrl.getModel();
		if (model.getMagicOrResonanceType()!=null && model.getMagicOrResonanceType().usesPowers() && model.getMagicOrResonanceType().usesSpells()) {
			logger.log(Level.WARNING, "Mentor spirit magic user: {0}", model.getMagicOrResonanceType());
			addLabel(ResourceI18N.get(RES, "choice.magician_adept"));

			ChoiceBox<MagicOrResonanceType> cbMagOrAdp = new ChoiceBox<>();
			cbMagOrAdp.getItems().add(Shadowrun6Core.getItem(MagicOrResonanceType.class, "magician"));
			cbMagOrAdp.getItems().add(Shadowrun6Core.getItem(MagicOrResonanceType.class, "adept"));
			cbMagOrAdp.setConverter(new StringConverter<MagicOrResonanceType>() {
				public String toString(MagicOrResonanceType v) {return (v!=null)?v.getName():null;}
				public MagicOrResonanceType fromString(String string) {return null;}
			});
			content.getChildren().add(cbMagOrAdp);
			cbMagOrAdp.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
				logger.log(Level.DEBUG, "Use advantages for "+n);
				decisions.put(magicianOrAdept, new Decision(magicianOrAdept, n.getId()));
				chooseAdeptAdvantages.setValue (n!=null && n.usesPowers());
				updateButtons();
			});
			cbMagOrAdp.getSelectionModel().select(0);
		}

		return cbMentor;
	}

	//-------------------------------------------------------------------
	private Node handleSUBSELECT(ComplexDataItem item, Choice choice) {
		ChoiceBox<ChoiceOption> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<ChoiceOption>() {
			public ChoiceOption fromString(String value) { return null;}
			public String toString(ChoiceOption value) {
				if (value==null) return "-";
				String name = item.getChoiceOptionStrings(choice, value, Locale.getDefault())[0];
				if (value.getCost()!=0) {
					if (item instanceof ItemTemplate) {
						if ( Math.round(value.getCost())==value.getCost()) {
							name+=" (+"+((int)(value.getCost()))+" Nuyen)";
						} else {
							name+=" ("+value.getCost()+" Nuyen)";
						}
					} else {
						if ( Math.round(value.getCost())==value.getCost()) {
							name+=" (+"+((int)(value.getCost()))+" Karma)";
						} else {
							name+=" ("+value.getCost()+" Karma)";
						}
					}
				}
				return name;
			}
		});
		for (ChoiceOption opt : choice.getSubOptions()) {
			logger.log(Level.DEBUG, "  sub option "+opt);
			cbSub.getItems().add(opt);

		}
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));
			updateButtons();
			showHelpFor(n); });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleTEXT(ComplexDataItem item, Choice choice) {
		TextField tfDescr = new TextField();
		content.getChildren().add(tfDescr);
		tfDescr.textProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n));
			updateButtons();
		});
		return tfDescr;
	}

	//-------------------------------------------------------------------
	private Node handleGEAR(ComplexDataItem item, Choice choice) {
		ChoiceBox<ItemTemplate> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<ItemTemplate>() {
			public ItemTemplate fromString(String value) { return null;}
			public String toString(ItemTemplate value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		List<ItemTemplate> items = new ArrayList<>();
		List<CarriedItem<ItemTemplate>> carried = ((Shadowrun6Character)ctrl.getModel()).getCarriedItemsRecursive();
		// If options are given, use those
		if (choice.getChoiceOptions()!=null && choice.getChoiceOptions().length>0) {
			List<ItemTemplate> overwrite = new ArrayList<>();
			for (String key : choice.getChoiceOptions()) {
				ItemTemplate resolved = Shadowrun6Core.getItem(ItemTemplate.class, key);
				if (resolved==null)
					throw new NullPointerException("No such gear '"+key+"'");
				overwrite.add(resolved);
			}
			items.clear();
			items.addAll(overwrite);
		} else {
			items = Shadowrun6Core.getItemList(ItemTemplate.class).stream().filter(data -> ctrl.getCharacterController().showDataItem(data)).collect(Collectors.toList());
		}

		Predicate<ItemTemplate> filterIsCarried = new Predicate<ItemTemplate>() {
			public boolean test(ItemTemplate item) {
				return (carried.stream().anyMatch(ci -> ci.getResolved()==item)) ;
			}

		};

		// Eventually sort
		if (choice.getTypeReference()!=null) {
			logger.log(Level.DEBUG, "Reduce gear to select from to "+choice.getTypeReference());
			switch (choice.getTypeReference()) {
			case "MELEE":
				items = items.stream().filter(i -> i.getAttribute(SR6ItemAttribute.ITEMTYPE).getValue()==ItemType.WEAPON_CLOSE_COMBAT).collect(Collectors.toList());
				System.err.println("ChoiceSelectorDialog: items = "+items);
				break;
			case "CHEMICALS":
				items = Shadowrun6Core.getItemList(ItemTemplate.class);
				items = items.stream().filter(i -> i.getAttribute(SR6ItemAttribute.ITEMTYPE).getValue()==ItemType.CHEMICALS).collect(Collectors.toList());
				break;
			case "PARENT_OR_ALTERNATES":
				logger.log(Level.WARNING, "TODO: Check this for item detection");
				System.err.println("ChoiceSelectorDialog: TODO: Check this for item detection");
				if (context!=null && context.getResolved() instanceof ItemTemplate) {
					ItemTemplate temp = (ItemTemplate)context.getResolved();
					items.add(temp);
					items.addAll(temp.getGearSubDefinitions());
				}
				break;
			case "VEHICLE":
				// Used for maneuvering autosoft
				logger.log(Level.ERROR, "Reduce {0} gear items",items.size());
				items = items.stream()
						.filter(i -> ItemType.isVehicle(i.getAttribute(SR6ItemAttribute.ITEMTYPE).getValue()))
						.filter(filterIsCarried)
						.collect(Collectors.toList());
				logger.log(Level.ERROR, "Reduce GEAR to ''{0}''.\nAll vehicles are {1} results",choice.getTypeReference(),items.size());
				break;
			case "WEAPON":
				// Used for targeting autosoft
				items = items.stream()
						.filter(i -> ItemType.isWeapon(i))
						.filter(filterIsCarried)
						.collect(Collectors.toList());
				logger.log(Level.ERROR, "Don't know how to reduce GEAR to '"+choice.getTypeReference()+"'.\nAll weapons are {0} results",items.size());
				break;
			default:
				logger.log(Level.ERROR, "Don't know how to reduce GEAR to '"+choice.getTypeReference()+"'");
				System.err.println("ChoiceSelectorDialog: Don't know how to reduce gear to select from to "+choice.getTypeReference());
			}
		}
		if (items.isEmpty()) {
			System.err.println("PROBLEM: Reducing gear selection to empty list");
			logger.log(Level.ERROR, "PROBLEM: Reducing gear selection to empty list");
			BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 1, "Selecting all "+choice.getTypeReference()+" returns an empty list.");
		}
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<ItemTemplate>() {
			public int compare(ItemTemplate o1, ItemTemplate o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.WARNING, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleCARRIED(ComplexDataItem item, Choice choice) {
		ChoiceBox<CarriedItem<ItemTemplate>> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<CarriedItem<ItemTemplate>>() {
			public CarriedItem<ItemTemplate> fromString(String value) { return null;}
			public String toString(CarriedItem<ItemTemplate> value) {
				if (value==null) return "-";
				return value.getNameWithRating();
			}
		});
		List<CarriedItem<ItemTemplate>> items = ((Shadowrun6Character)ctrl.getModel()).getCarriedItems();

		// Eventually sort
		if (choice.getTypeReference()!=null) {
			logger.log(Level.DEBUG, "Reduce gear to select from to "+choice.getTypeReference());
			System.err.println("Reduce gear to select from to "+choice.getTypeReference());
			switch (choice.getTypeReference()) {
			case "MELEE":
				items = items.stream().filter(i -> i.getAsObject(SR6ItemAttribute.ITEMTYPE).getValue()==ItemType.WEAPON_CLOSE_COMBAT).collect(Collectors.toList());
				break;
			default:
				logger.log(Level.WARNING, "Don't know how to reduce GEAR to '"+choice.getTypeReference()+"'");
			}
		}
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<CarriedItem<ItemTemplate>>() {
			public int compare(CarriedItem<ItemTemplate> o1, CarriedItem<ItemTemplate> o2) {
				return Collator.getInstance().compare(o1.getNameWithRating(), o2.getNameWithRating());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getUuid().toString()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleCONTACT(ComplexDataItem item, Choice choice) {
		ChoiceBox<Contact> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<Contact>() {
			public Contact fromString(String value) { return null;}
			public String toString(Contact value) {
				if (value==null) return "-";
				return value.getName()+" ( "+value.getTypeName()+")";
			}
		});
		List<Contact> items = ((Shadowrun6Character)ctrl.getModel()).getContacts();

//		// Eventually sort
//		if (choice.getTypeReference()!=null) {
//			logger.log(Level.DEBUG, "Reduce contact to select from to "+choice.getTypeReference());
//			System.err.println("Reduce contact to select from to "+choice.getTypeReference());
//			switch (choice.getTypeReference()) {
//			case "MELEE":
//				items = items.stream().filter(i -> i.getAsObject(SR6ItemAttribute.ITEMTYPE).getValue()==ItemType.WEAPON_CLOSE_COMBAT).collect(Collectors.toList());
//				break;
//			default:
//				logger.log(Level.WARNING, "Don't know how to reduce GEAR to '"+choice.getTypeReference()+"'");
//			}
//		}
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<Contact>() {
			public int compare(Contact o1, Contact o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId().toString()));

			updateButtons();
			//showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handlePROGRAM(ComplexDataItem item, Choice choice) {
		ChoiceBox<ItemTemplate> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<ItemTemplate>() {
			public ItemTemplate fromString(String value) { return null;}
			public String toString(ItemTemplate value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		List<ItemTemplate> items = Shadowrun6Core.getItemList(ItemTemplate.class)
				.stream()
				.filter(new ItemTypeFilter(CarryMode.EMBEDDED, ItemType.SOFTWARE))
				.toList();
		// Eventually sort
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<ItemTemplate>() {
			public int compare(ItemTemplate o1, ItemTemplate o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleQUALITY(ComplexDataItem item, Choice choice) {
		ChoiceBox<SR6Quality> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<SR6Quality>() {
			public SR6Quality fromString(String value) { return null;}
			public String toString(SR6Quality value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		List<SR6Quality> items = Shadowrun6Core.getItemList(SR6Quality.class)
				.stream()
				.filter( q -> (choice.getChoiceOptions()==null) || List.of(choice.getChoiceOptions()).contains(q.getId()))
				.filter( q -> matchesQualityChoiceReference(q, choice.getTypeReference()))
				.toList();
		// Eventually sort
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<Quality>() {
			public int compare(Quality o1, Quality o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
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
	private Node handleSignatureManeuvers(ComplexDataItem item, Choice choice) {
		ChoiceBox<SignatureManeuver> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<SignatureManeuver>() {
			public SignatureManeuver fromString(String value) { return null;}
			public String toString(SignatureManeuver value) {
				if (value==null) return "-";
				return value.getName();
			}
		});

		List<SignatureManeuver> items = (model!=null)?model.getSignatureManeuvers():List.of();
		// Eventually sort
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<SignatureManeuver>() {
			public int compare(SignatureManeuver o1, SignatureManeuver o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getUuid().toString()));

			updateButtons();
			//showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleSPELL_CATEGORY(ComplexDataItem item, Choice choice) {
		ChoiceBox<ASpell.Category> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<ASpell.Category>() {
			public ASpell.Category fromString(String value) { return null;}
			public String toString(ASpell.Category value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
					List.of(ASpell.Category.values()).stream().filter(s -> ids.contains(s.name())).collect(Collectors.toList())
					);
		} else {
			cbSub.getItems().addAll(ASpell.Category.values());
		}
		Collections.sort(cbSub.getItems(), new Comparator<ASpell.Category>() {
			public int compare(ASpell.Category o1, ASpell.Category o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.name()));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleSPELL(ComplexDataItem item, Choice choice) {
		ChoiceBox<SR6Spell> choicebox = new ChoiceBox<>();
		choicebox.setConverter(new StringConverter<SR6Spell>() {
			public SR6Spell fromString(String value) { return null;}
			public String toString(SR6Spell value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		List<SR6Spell> items = Shadowrun6Core.getItemList(SR6Spell.class)
				.stream()
				.filter( q -> (choice.getChoiceOptions()==null) || List.of(choice.getChoiceOptions()).contains(q.getId()))
				.toList();
		// Eventually sort
		choicebox.getItems().addAll(items);

		Collections.sort(choicebox.getItems(), new Comparator<SR6Spell>() {
			public int compare(SR6Spell o1, SR6Spell o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		choicebox.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(choicebox);

		return choicebox;
	}

	//-------------------------------------------------------------------
	private Node handleSENSE(ComplexDataItem item, Choice choice) {
		ChoiceBox<Sense> cbSub = new ChoiceBox<>();
		cbSub.setConverter(new StringConverter<Sense>() {
			public Sense fromString(String value) { return null;}
			public String toString(Sense value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		// All but only given options?
		if (choice.getChoiceOptions()!=null) {
			List<String> ids = List.of(choice.getChoiceOptions());
			cbSub.getItems().addAll(
					Shadowrun6Core.getItemList(Sense.class).stream().filter(s -> ids.contains(s.getId())).collect(Collectors.toList())
					);
		} else {
			cbSub.getItems().addAll(Shadowrun6Core.getItemList(Sense.class));
		}
		Collections.sort(cbSub.getItems(), new Comparator<Sense>() {
			public int compare(Sense o1, Sense o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});
		cbSub.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getName()));
			updateButtons();
		 });
		content.getChildren().add(cbSub);
		return cbSub;
	}

	//-------------------------------------------------------------------
	private Node handleACTION(ComplexDataItem item, Choice choice) {
		ChoiceBox<Shadowrun6Action> cbAmmoType = new ChoiceBox<>();
		cbAmmoType.setConverter(new StringConverter<Shadowrun6Action>() {
			public Shadowrun6Action fromString(String value) { return null;}
			public String toString(Shadowrun6Action value) {
				if (value==null) return "-";
				return value.getName();
			}
		});
		if (choice.getChoiceOptions()!=null && choice.getChoiceOptions().length>0) {
			// Specific actions
			List<String> options = List.of(choice.getChoiceOptions());
			for (Shadowrun6Action tmp : Shadowrun6Core.getItemList(Shadowrun6Action.class)) {
				if (options.contains(tmp.getId()))
					cbAmmoType.getItems().add(tmp);
			}
		} else {
			// All actions unfiltered
			cbAmmoType.getItems().addAll(Shadowrun6Core.getItemList(Shadowrun6Action.class));
		}
		Collections.sort(cbAmmoType.getItems(), new Comparator<Shadowrun6Action>() {
			public int compare(Shadowrun6Action o1, Shadowrun6Action o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}});


		cbAmmoType.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.DEBUG, "Chose {0} for {1}", n, choice.getUUID());
			decisions.put(choice, new Decision(choice, n.getId()));

			updateButtons();
			showHelpFor(n);
		 });
		content.getChildren().add(cbAmmoType);

		return cbAmmoType;
	}

	//-------------------------------------------------------------------
	public void setSuggestedVariant(SR6PieceOfGearVariant variant) {
		this.suggestedVariant = variant;
	}

}
