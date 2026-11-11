package de.rpgframework.shadowrun6.chargen.jfx.wizard;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.prelle.javafx.AlertManager;
import org.prelle.javafx.AlertType;
import org.prelle.javafx.CloseType;
import org.prelle.javafx.FlexibleApplication;
import org.prelle.javafx.ManagedDialog;
import org.prelle.javafx.OptionalNodePane;
import org.prelle.javafx.ResponsiveControlManager;
import org.prelle.javafx.WindowMode;
import org.prelle.javafx.Wizard;
import org.prelle.javafx.WizardPage;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.BasicControllerEvents;
import de.rpgframework.genericrpg.chargen.ControllerEvent;
import de.rpgframework.genericrpg.chargen.ControllerListener;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.jfx.ComplexDataItemControllerNode;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.jfx.wizard.NumberUnitBackHeader;
import de.rpgframework.shadowrun.BodyType;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.Quality.QualityType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IQualityController;
import de.rpgframework.shadowrun.chargen.charctrl.IShadowrunCharacterController;
import de.rpgframework.shadowrun.chargen.charctrl.IShadowrunCharacterControllerProvider;
import de.rpgframework.shadowrun.chargen.jfx.listcell.QualityListCell;
import de.rpgframework.shadowrun.chargen.jfx.listcell.QualityValueListCell;
import de.rpgframework.shadowrun.chargen.jfx.pane.QualitySelector;
import de.rpgframework.shadowrun.chargen.jfx.wizard.AWizardPageQualities;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.GeneratorWrapper;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.BornThisWayGenerator;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathSettings;
import de.rpgframework.shadowrun6.chargen.jfx.QualityFilterNode;
import de.rpgframework.shadowrun6.chargen.jfx.selector.ChoiceSelectorDialog;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author prelle
 *
 */
public class SR6WizardPageBornThisWay extends WizardPage implements ControllerListener {

	private final static Logger logger = System.getLogger(AWizardPageQualities.class.getPackageName());

	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6WizardPageBornThisWay.class.getPackageName()+".SR6WizardPages");

	protected SR6CharacterGenerator charGen;
	protected BornThisWayGenerator bornThisWay;

	private Label lbMetatype, lbMOR, lbQuality;

	private TextField tfNationality;
	private TextField tfNativeLanguage;

	private TextArea taDescription;
	private QualityFilterNode filter;
	protected ComplexDataItemControllerNode<Quality, QualityValue> selection;

	protected GenericDescriptionVBox bxDescription;
	protected OptionalNodePane layout;
	private NumberUnitBackHeader backHeader;

	//-------------------------------------------------------------------
	public SR6WizardPageBornThisWay(Wizard wizard, GeneratorWrapper charGen) {
		super(wizard);
		this.charGen = charGen;
		setTitle(ResourceI18N.get(RES, "page.born_this_way.title"));
		initComponents();
		initLayout();
		initInteractivity();

		bornThisWay = ((GeneratorWrapper)charGen).getWrapped().getBornThisWayGenerator();

		charGen.addListener(this);
	}

	//-------------------------------------------------------------------
	protected void initComponents() {
		lbMetatype = new Label();
		lbMOR      = new Label();
		tfNationality = new TextField();
		tfNativeLanguage = new TextField();

		taDescription = new TextArea();
		taDescription.setWrapText(true);
		taDescription.setMaxWidth(450);
		taDescription.setPromptText(ResourceI18N.get(RES, "page.born_this_way.prompt"));
		bxDescription = new GenericDescriptionVBox(Shadowrun6Tools.requirementResolver(Locale.getDefault()), Shadowrun6Tools.modificationResolver(Locale.getDefault()));

		selection = new ComplexDataItemControllerNode<>(charGen.getQualityController());
		selection.setAvailableStyle("-fx-min-width: 20em; -fx-max-width: 25em");
		selection.setSelectedStyle("-fx-min-width: 20em; -fx-max-width: 30em; -fx-max-height: 10em");

		int maximumQualities = getMaximumBornQualities();
		selection.setAvailablePlaceholder(ResourceI18N.format(RES, "page.born_this_way.placeholder.available", maximumQualities));
		selection.setSelectedPlaceholder(ResourceI18N.format(RES, "page.born_this_way.placeholder.selected", maximumQualities));

		selection.setAvailableCellFactory(lv -> new QualityListCell(selection.getController(), Shadowrun6Tools.requirementResolver(Locale.getDefault())));
		selection.setSelectedCellFactory(lv -> new QualityValueListCell( ()->charGen, true));
		selection.setShowHeadings(ResponsiveControlManager.getCurrentMode()!=WindowMode.MINIMAL);
		selection.setRequirementResolver(Shadowrun6Tools.requirementResolver(Locale.getDefault()));
		selection.setModificationResolver(Shadowrun6Tools.modificationResolver(Locale.getDefault()));
		selection.setShowHeadings(false);
	}

	//-------------------------------------------------------------------
	protected void initLayout() {

		backHeader = new NumberUnitBackHeader("Karma");
		backHeader.setValue(charGen.getModel().getKarmaFree());
		HBox.setMargin(backHeader, new Insets(0,10,0,10));
		super.setBackHeader(backHeader);

		selection.setMaxWidth(600);

		FlowPane flowLang = new FlowPane(5, 5, new Label(ResourceI18N.get(RES, "page.born_this_way.language")), tfNativeLanguage);

		lbQuality = new Label();

		Label lbSummarize = new Label(ResourceI18N.get(RES, "page.born_this_way.summarize"));
		VBox postList = new VBox(5, lbSummarize, taDescription);
		VBox content = new VBox(10, lbMOR, lbMetatype, flowLang, lbQuality, selection);

		selection.setSelectedListPostNode(postList);
		layout = new OptionalNodePane(content, bxDescription);
		setContent(layout);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
//		btnQuality1.setOnAction(e -> handleSelect(bornThisWay.getQualityController1()));
//		btnQuality2.setOnAction(e -> handleSelect(bornThisWay.getQualityController2()));
		selection.showHelpForProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "show help for "+n);
			bxDescription.setData(n);
			if (n!=null) {
				layout.setTitle(n.getName());
			} else {
				layout.setTitle(null);
			}
		});
		taDescription.textProperty().addListener( (ov,o,n) -> {
			charGen.getModel().setLifepathBabyBackground(n);
		});
		tfNativeLanguage.textProperty().addListener( (ov,o,n) -> {
			bornThisWay.selectNativeLanguage(n);
		});
	}

	//-------------------------------------------------------------------
	private void handleSelect(IQualityController ctrl) {
		QualitySelector selector = new SearchableQualitySelector(
				ctrl,
				Shadowrun6Tools.requirementResolver(Locale.getDefault()),
				Shadowrun6Tools.modificationResolver(Locale.getDefault())
				);
		ManagedDialog dialog = new ManagedDialog(ResourceI18N.get(RES,"section.quality.selector.title"), selector, CloseType.OK, CloseType.CANCEL);

		CloseType close = (CloseType) FlexibleApplication.getInstance().showAndWait(dialog);
		logger.log(Level.ERROR,"Closed with "+close);
		if (close==CloseType.OK) {
			Quality toSelect = selector.getSelected();
			Possible possible = ctrl.canBeSelected(toSelect);
			logger.log(Level.ERROR, "possible = "+possible);
			if (possible.get()) {
				// Is there a need for a selection
				logger.log(Level.ERROR, "ctrl = " + ctrl);
				if (!ctrl.getChoicesToDecide(toSelect).isEmpty()) {
					// Yes, user must choose
					List<Choice> options = ctrl.getChoicesToDecide(toSelect);
					logger.log(Level.ERROR, "called getChoicesToDecide returns {0} choices", options.size());
					ChoiceSelectorDialog<Quality, QualityValue> choiceDialog = new ChoiceSelectorDialog<Quality, QualityValue>(ctrl);
					Decision[] decisions = choiceDialog.apply(toSelect, options);
					if (decisions != null) {
						logger.log(Level.ERROR, "call select(option, decision[{0}])", decisions.length);
						OperationResult<QualityValue> res = ctrl.select(toSelect, decisions);
						if (res.wasSuccessful()) {
							logger.log(Level.ERROR, "Selecting {0} with options was successful", toSelect);
						} else {
							logger.log(Level.ERROR, "Selecting {0} with options failed: {1}", toSelect, res.getError());
							AlertManager.showAlertAndCall(javafx.scene.control.Alert.AlertType.ERROR, "Failed adding", res.getError());
						}
					}
				} else {
					// No
					logger.log(Level.DEBUG, "call select(option)");
					OperationResult<QualityValue> res = ctrl.select(toSelect);
					if (res.wasSuccessful()) {
						logger.log(Level.INFO, "Selecting {0} was successful", toSelect);
					} else {
						logger.log(Level.WARNING, "Selecting {0} failed: {1}", toSelect, res.getError());
						AlertManager.showAlertAndCall(javafx.scene.control.Alert.AlertType.ERROR, "Failed adding", res.getError());
					}
				}
			} else {
				logger.log(Level.DEBUG, "can not be Selected(" + toSelect + "): " + possible.getI18NKey());

	    		FlexibleApplication.getInstance().showAlertAndCall(AlertType.NOTIFICATION, "Selection failed", possible.toString());
	    	}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.javafx.WizardPage#pageVisited()
	 */
	@Override
	public void pageVisited() {
		logger.log(Level.INFO, "pageVisited");
		refresh();
	}

	//-------------------------------------------------------------------
	protected void refresh() {
		boolean isLifepath =  charGen.getId().equals("lifepath");
		activeProperty().set( isLifepath );
		if (!isLifepath)
			return;

		backHeader.setValue(charGen.getModel().getKarmaFree());
		setTextIfChanged(taDescription, charGen.getModel().getLifepathBabyBackground());
		String mor = (charGen.getModel().getMagicOrResonanceType()!=null)?charGen.getModel().getMagicOrResonanceType().getName():"?";
		lbMOR.setText(ResourceI18N.format(RES, "page.born_this_way.mor", mor));
		int maximumQualities = getMaximumBornQualities();
		lbQuality.setText(ResourceI18N.format(RES, "page.born_this_way.quality", maximumQualities));
		selection.setAvailablePlaceholder(ResourceI18N.format(RES, "page.born_this_way.placeholder.available", maximumQualities));
		selection.setSelectedPlaceholder(ResourceI18N.format(RES, "page.born_this_way.placeholder.selected", maximumQualities));
		if (isLifepath) {
			SR6LifePathSettings settings = charGen.getModel().getCharGenSettings(SR6LifePathSettings.class);
			String meta = (charGen.getModel().getMetatype()!=null)?charGen.getModel().getMetatype().getName():"?";
			lbMetatype.setText(ResourceI18N.format(RES, "page.born_this_way.metatype", meta, "NATION", settings.getNativeLanguage()));
		}

		selection.refresh();
	}

	//-------------------------------------------------------------------
	private int getMaximumBornQualities() {
		if (bornThisWay!=null)
			return bornThisWay.getMaximumBornQualities();
		BornThisWayGenerator controller = charGen.getBornThisWayGenerator();
		return controller!=null?controller.getMaximumBornQualities():0;
	}

	//-------------------------------------------------------------------
	private void setTextIfChanged(TextArea area, String value) {
		String text = value!=null?value:"";
		if (!text.equals(area.getText()))
			area.setText(text);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.chargen.ControllerListener#handleControllerEvent(de.rpgframework.genericrpg.chargen.ControllerEvent, java.lang.Object[])
	 */
	@Override
	public void handleControllerEvent(ControllerEvent type, Object... param) {
		logger.log(Level.WARNING, "RCV {0}",type);
		logger.log(Level.INFO, "RCV " + type + " with " + Arrays.toString(param));

		if (type == BasicControllerEvents.GENERATOR_CHANGED) {
			logger.log(Level.INFO, "RCV " + type + " with " + Arrays.toString(param));
			charGen = (SR6CharacterGenerator) param[0];
			bornThisWay =  ((SR6CharacterGenerator)charGen).getBornThisWayGenerator();
			if (bornThisWay!=null) {
				selection.setController(bornThisWay.getQualityController());
				selection.setOptionCallback(new ChoiceSelectorDialog<>(charGen.getQualityController()));
				filter = new QualityFilterNode(RES, selection, QualityType.NORMAL);
				selection.setFilterNode(filter);
				activeProperty().set( true );
			} else {
				activeProperty().set( false );
			}
		}
		if (type==BasicControllerEvents.CHARACTER_CHANGED) {
			refresh();
		}
//		if (type==BasicControllerEvents.CHARACTER_CHANGED || type==BasicControllerEvents.GENERATOR_CHANGED) {
//			paneAttr.setController(charGen.getDrakeController());
//			selection.refresh();
//			refresh();
//		}
	}

}
