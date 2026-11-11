package de.rpgframework.shadowrun6.chargen.jfx.wizard;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.prelle.javafx.AlertManager;
import org.prelle.javafx.AlertType;
import org.prelle.javafx.CloseType;
import org.prelle.javafx.FlexibleApplication;
import org.prelle.javafx.ManagedDialog;
import org.prelle.javafx.OptionalNodePane;
import org.prelle.javafx.SymbolIcon;
import org.prelle.javafx.Wizard;
import org.prelle.javafx.WizardPage;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.chargen.BasicControllerEvents;
import de.rpgframework.genericrpg.chargen.ControllerEvent;
import de.rpgframework.genericrpg.chargen.ControllerListener;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.chargen.SingleComplexDataItemController;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.jfx.BlankableValueControl;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.jfx.wizard.NumberUnitBackHeader;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.chargen.charctrl.IQualityController;
import de.rpgframework.shadowrun.chargen.jfx.listcell.QualityValueListCell;
import de.rpgframework.shadowrun.chargen.jfx.pages.FilterQualities;
import de.rpgframework.shadowrun.chargen.jfx.pane.QualitySelector;
import de.rpgframework.shadowrun.chargen.jfx.wizard.AWizardPageQualities;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.GeneratorWrapper;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.ChildhoodGenerator;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.ChildhoodGenerator.SimpleSkillController;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathSettings;
import de.rpgframework.shadowrun6.chargen.jfx.listcell.SelectedSkillListCell;
import de.rpgframework.shadowrun6.chargen.jfx.selector.ChoiceSelectorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 * @author prelle
 *
 */
public class SR6WizardPageLPChildhood extends WizardPage implements ControllerListener {

	private final static Logger logger = System.getLogger(AWizardPageQualities.class.getPackageName());

	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6WizardPageLPChildhood.class.getPackageName()+".SR6WizardPages");

	protected SR6CharacterGenerator charGen;
	protected ChildhoodGenerator childhood;

	private TilePane tpSkills;
	private ListView<SR6Skill> lvSkills;
	private ListView<QualityValue> lvQualities;
	protected Button btnAdd;
	protected Button btnDel;
	private TextField tfArea;
	private TextArea taDescription;
	private Map<SR6Skill, ToggleButton> skillButtons;
	private BlankableValueControl<Quality, QualityValue> posQuality;
	private BlankableValueControl<Quality, QualityValue> negQuality;

	protected GenericDescriptionVBox bxDescription;
	protected OptionalNodePane layout;
	private NumberUnitBackHeader backHeader;

	//-------------------------------------------------------------------
	public SR6WizardPageLPChildhood(Wizard wizard, GeneratorWrapper charGen) {
		super(wizard);
		this.charGen = charGen;
		setTitle(ResourceI18N.get(RES, "page.childhood.title"));
		initComponents();
		initLayout();
		initInteractivity();

		childhood = ((GeneratorWrapper)charGen).getWrapped().getChildhoodGenerator();

		charGen.addListener(this);
	}

	//-------------------------------------------------------------------
	protected void initComponents() {
		skillButtons = new HashMap<>();
		tpSkills = new TilePane(20,20);
		tpSkills.setPrefColumns(4);
		updateSkillButtons();

		posQuality = new BlankableValueControl<>(ResourceI18N.get(RES, "page.childhood.quality.positive"));
		negQuality = new BlankableValueControl<>(ResourceI18N.get(RES, "page.childhood.quality.negative"));
		posQuality.setController(new SingleComplexDataItemController<Quality, QualityValue>() {
			public void selectClicked() {
				logger.log(Level.WARNING, "selectClicked on "+posQuality.getSelected());
				if (posQuality.getSelected()!=null) {
					childhood.getQualityController().deselect(posQuality.getSelected());
					posQuality.setSelected(null);
				} else {
					QualityValue val = onAddQuality(true);
					posQuality.setSelected(val);
				}
			}
			public OperationResult<QualityValue> select(Quality value, Decision... decisions) {
				logger.log(Level.WARNING, "Select "+value);
				OperationResult<QualityValue> res = childhood.getQualityController().select(value, decisions);
				posQuality.setSelected(res.get());
				return res;
			}
			public boolean deselect(QualityValue value) {
				return childhood.getQualityController().deselect(value);
			}
			public boolean canBeUsed() { return true;}
			public Possible canBeDeselected(QualityValue value) {
				return childhood.getQualityController().canBeDeselected(value);
			}
		});
		negQuality.setController(new SingleComplexDataItemController<Quality, QualityValue>() {
			public void selectClicked() {
				SR6LifePathSettings settings = charGen.getModel().getCharGenSettings(SR6LifePathSettings.class);
				if (negQuality.getSelected()!=null) {
					childhood.getQualityController().deselect(negQuality.getSelected());
					negQuality.setSelected(null);
				} else {
					QualityValue val = onAddQuality(false);
					negQuality.setSelected(val);
				}
			}
			public OperationResult<QualityValue> select(Quality value, Decision... decisions) {
				// TODO Auto-generated method stub
				return null;
			}
			public boolean deselect(QualityValue value) {
				return childhood.getQualityController().deselect(value);
			}
			public boolean canBeUsed() { return true;}
			public Possible canBeDeselected(QualityValue value) {
				return childhood.getQualityController().canBeDeselected(value);
			}
		});

		Label phQualities = new Label(ResourceI18N.get(RES, "page.childhood.qualities.placeholder"));
		phQualities.setWrapText(true);
		lvSkills = new ListView<>();
		lvSkills.setCellFactory(lv -> new SelectedSkillListCell( () -> childhood.getSkillController()));
		btnAdd = new Button(null, new SymbolIcon("add"));
		btnDel = new Button(null, new SymbolIcon("delete"));
		btnDel.setDisable(true);
		lvQualities = new ListView<>();
		lvQualities.setPlaceholder(phQualities);
		lvQualities.setCellFactory(lv -> new QualityValueListCell( () -> charGen, true));
		tfArea = new TextField();

		taDescription = new TextArea();
		taDescription.setWrapText(true);
		taDescription.setMaxWidth(450);
		taDescription.setPromptText(ResourceI18N.get(RES, "page.childhood.prompt"));
		bxDescription = new GenericDescriptionVBox(Shadowrun6Tools.requirementResolver(Locale.getDefault()), Shadowrun6Tools.modificationResolver(Locale.getDefault()));
	}

	//-------------------------------------------------------------------
	protected void initLayout() {
		TilePane tpQualities = new TilePane(20,20, posQuality, negQuality);
		posQuality.setMaxWidth(Double.MAX_VALUE);
		negQuality.setMaxWidth(Double.MAX_VALUE);

		backHeader = new NumberUnitBackHeader("Karma");
		backHeader.setValue(charGen.getModel().getKarmaFree());
		HBox.setMargin(backHeader, new Insets(0,10,0,10));
		super.setBackHeader(backHeader);

		Label lbSkills = new Label(ResourceI18N.get(RES, "page.childhood.skills"));
		lbSkills.setWrapText(true);
		VBox bxSkills = new VBox(10, lbSkills, lvSkills);
		lvSkills.setStyle("-fx-pref-width: 12em; -fx-max-width: 16emx; -fx-max-height: 18em");

		Label lbQuality = new Label(ResourceI18N.get(RES, "page.childhood.quality"));
		lbQuality.setWrapText(true);
		HBox bxButtons = new HBox(10, btnAdd, btnDel);
		bxButtons.setAlignment(Pos.CENTER_RIGHT);
		VBox bxQualities = new VBox(10, lbQuality, bxButtons, lvQualities);
		bxQualities.setMaxWidth(420);
		bxQualities.setStyle("-fx-pref-width: 20em; -fx-max-width: 420px; -fx-max-height: 18em");

		Label lbSummarize = new Label(ResourceI18N.get(RES, "page.childhood.summarize"));
		Label lbArea      = new Label(ResourceI18N.get(RES, "page.childhood.area"));
		VBox bxFluff = new VBox(5, lbArea, tfArea, lbSummarize, taDescription);
		VBox content = new VBox(20,lbSkills,tpSkills, lbQuality, tpQualities, bxFluff);

		layout = new OptionalNodePane(content, bxDescription);
		setContent(layout);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
//		btnQuality1.setOnAction(e -> handleSelect(bornThisWay.getQualityController1()));
//		btnQuality2.setOnAction(e -> handleSelect(bornThisWay.getQualityController2()));
		lvSkills.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "show help for "+n);
			bxDescription.setData(n);
			if (n!=null) {
				layout.setTitle(n.getName());
			} else {
				layout.setTitle(null);
			}
		});
		lvQualities.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "show help for "+n);
			bxDescription.setData(n);
			btnDel.setDisable(n==null);
			if (n!=null) {
				layout.setTitle(n.getName());
			} else {
				layout.setTitle(null);
			}
		});
		taDescription.textProperty().addListener( (ov,o,n) -> {
			charGen.getModel().setLifepathChildhoodBackground(n);
		});
		tfArea.textProperty().addListener( (ov,o,n) -> {
			childhood.selectChildhoodArea(n);
		});

//		btnAdd.setOnAction(ev -> onAddQuality());
		btnDel.setOnAction(ev -> onDeleteQuality());
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
	private void updateSkillButtons() {
		tpSkills.getChildren().clear();
		if (childhood==null) return;
		List<SR6Skill> skills = new ArrayList<>(childhood.getAvailableSkills());
		Collections.sort(skills, new Comparator<SR6Skill>() {
			public int compare(SR6Skill s1, SR6Skill s2) {
				return s1.getName().compareTo(s2.getName());
			}
		});
		skills.forEach(skl -> {
			ToggleButton btn = new ToggleButton(skl.getName());
			btn.setUserData(skl);
			btn.setMaxWidth(Double.MAX_VALUE);
			btn.setOnAction(ev -> {
				logger.log(Level.INFO, "Clicked skill {0}", skl);
				if (childhood.getSkillController().isSelected(skl)) {
					logger.log(Level.DEBUG, "Deselect skill {0}", skl);
					childhood.getSkillController().deselect(skl);
				} else {
					logger.log(Level.DEBUG, "Select skill {0}", skl);
					childhood.getSkillController().select(skl);
					refresh();
				}
			});
			skillButtons.put(skl, btn);
			tpSkills.getChildren().add(btn);
		});
	}

	//-------------------------------------------------------------------
	protected void refresh() {
		boolean isLifepath =  charGen.getId().equals("lifepath");
		activeProperty().set( isLifepath );
		if (!isLifepath)
			return;

		backHeader.setValue(charGen.getModel().getKarmaFree());
		setTextIfChanged(taDescription, charGen.getModel().getLifepathChildhoodBackground());


		lvSkills.getItems().setAll(childhood.getAvailableSkills());
		lvQualities.getItems().setAll(childhood.getQualityController().getSelected());
		SimpleSkillController sklCtrl = childhood.getSkillController();
		for (SR6Skill skl : childhood.getAvailableSkills()) {
			boolean isSelected = sklCtrl.isSelected(skl);
			skillButtons.get(skl).setSelected(isSelected);
			bxDescription.setData(skl);
			if (isSelected)
				skillButtons.get(skl).setDisable(false);
			else
				skillButtons.get(skl).setDisable(!sklCtrl.canBeSelected(skl));
		}
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
			childhood =  ((SR6CharacterGenerator)charGen).getChildhoodGenerator();
			if (childhood!=null) {
//				selection.setController(childhood.getQualityController());
//				selection.setOptionCallback(new ChoiceSelectorDialog<>(charGen.getQualityController()));
//				filter = new QualityFilterNode(RES, selection, QualityType.NORMAL);
//				selection.setFilterNode(filter);
				activeProperty().set( true );
				updateSkillButtons();
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

	//-------------------------------------------------------------------
	private QualityValue onAddQuality(boolean positive) {
		logger.log(Level.INFO, "onAddQuality");
		IQualityController ctrl = childhood.getQualityController();
		QualitySelector selector = new SearchableQualitySelector(
				ctrl,
				Shadowrun6Tools.requirementResolver(Locale.getDefault()),
				Shadowrun6Tools.modificationResolver(Locale.getDefault()),
				positive?FilterQualities.PosNeg.POSITIVE:FilterQualities.PosNeg.NEGATIVE
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
							return res.get();
						} else {
							logger.log(Level.ERROR, "Selecting {0} with options failed: {1}", toSelect, res.getError());
							AlertManager.showAlertAndCall(javafx.scene.control.Alert.AlertType.ERROR, "Failed adding", res.getError());
						}
					}
				} else {
					// No
					logger.log(Level.ERROR, "call select(option)");
					OperationResult<QualityValue> res = ctrl.select(toSelect);
					if (res.wasSuccessful()) {
						logger.log(Level.ERROR, "Selecting {0} was successful", toSelect);
						return res.get();
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
		return null;
	}

	//-------------------------------------------------------------------
	private void setTextIfChanged(TextArea area, String value) {
		String text = value!=null?value:"";
		if (!text.equals(area.getText()))
			area.setText(text);
	}

	//-------------------------------------------------------------------
	private void onDeleteQuality() {
		QualityValue val = lvQualities.getSelectionModel().getSelectedItem();
		childhood.getQualityController().deselect(val);
	}

}

