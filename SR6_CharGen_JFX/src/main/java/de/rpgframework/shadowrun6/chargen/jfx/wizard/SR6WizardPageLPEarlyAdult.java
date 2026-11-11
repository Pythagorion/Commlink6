package de.rpgframework.shadowrun6.chargen.jfx.wizard;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.prelle.javafx.AlertManager;
import org.prelle.javafx.AlertType;
import org.prelle.javafx.CloseType;
import org.prelle.javafx.FlexibleApplication;
import org.prelle.javafx.ManagedDialog;
import org.prelle.javafx.SymbolIcon;
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
import de.rpgframework.jfx.wizard.NumberUnitBackHeader;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ContactType;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.chargen.charctrl.IQualityController;
import de.rpgframework.shadowrun.chargen.jfx.listcell.QualityValueListCell;
import de.rpgframework.shadowrun.chargen.jfx.pane.QualitySelector;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.GeneratorWrapper;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.ChildhoodGenerator.SimpleSkillController;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.EarlyAdultGenerator;
import de.rpgframework.shadowrun6.chargen.jfx.selector.ChoiceSelectorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * @author prelle
 */
public class SR6WizardPageLPEarlyAdult extends WizardPage implements ControllerListener {

	private final static Logger logger = System.getLogger(SR6WizardPageLPEarlyAdult.class.getPackageName());
	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6WizardPageLPEarlyAdult.class.getPackageName()+".SR6WizardPages");

	protected SR6CharacterGenerator charGen;
	protected EarlyAdultGenerator earlyAdult;
	private TilePane tpSkills;
	private TilePane tpAttributes;
	private ListView<QualityValue> lvQualities;
	private Button btnAdd;
	private Button btnDel;
	private ChoiceBox<ContactType> cbContactType;
	private TextField tfContactName;
	private Spinner<Integer> spInfluence;
	private Label lbLoyalty;
	private TextArea taDescription;
	private NumberUnitBackHeader backHeader;
	/** Prevent listeners from writing back values while the page is refreshed */
	private boolean refreshing;

	//-------------------------------------------------------------------
	public SR6WizardPageLPEarlyAdult(Wizard wizard, GeneratorWrapper charGen) {
		super(wizard);
		this.charGen = charGen;
		this.earlyAdult = charGen.getWrapped().getEarlyAdultGenerator();
		setTitle(ResourceI18N.get(RES, "page.teenager.title"));
		initComponents();
		initLayout();
		initInteractivity();
		charGen.addListener(this);
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		tpSkills = new TilePane(10, 10);
		tpSkills.setPrefColumns(4);
		tpAttributes = new TilePane(10, 10);
		tpAttributes.setPrefColumns(4);

		btnAdd = new Button(null, new SymbolIcon("add"));
		btnDel = new Button(null, new SymbolIcon("delete"));
		btnDel.setDisable(true);
		Label phQualities = new Label(ResourceI18N.get(RES, "page.teenager.qualities.placeholder"));
		phQualities.setWrapText(true);
		lvQualities = new ListView<>();
		lvQualities.setPlaceholder(phQualities);
		lvQualities.setCellFactory(lv -> new QualityValueListCell( () -> charGen, true));

		cbContactType = new ChoiceBox<>();
		cbContactType.setConverter(new StringConverter<ContactType>() {
			public String toString(ContactType value) { return (value!=null)?value.getName():"-"; }
			public ContactType fromString(String string) { return null; }
		});
		tfContactName = new TextField();
		tfContactName.setPromptText(ResourceI18N.get(RES, "page.teenager.contact.name.prompt"));
		spInfluence = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3, 2));
		lbLoyalty = new Label();

		taDescription = new TextArea();
		taDescription.setWrapText(true);
		taDescription.setMaxWidth(450);
		taDescription.setPromptText(ResourceI18N.get(RES, "page.teenager.prompt"));
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		backHeader = new NumberUnitBackHeader("Karma");
		backHeader.setValue(charGen.getModel().getKarmaFree());
		HBox.setMargin(backHeader, new Insets(0,10,0,10));
		super.setBackHeader(backHeader);

		Label lbSkill = new Label(ResourceI18N.get(RES, "page.teenager.skill"));
		lbSkill.setWrapText(true);
		Label lbAttribute = new Label(ResourceI18N.get(RES, "page.teenager.attribute"));
		lbAttribute.setWrapText(true);

		ScrollPane skillScroll = new ScrollPane(tpSkills);
		skillScroll.setFitToWidth(true);
		skillScroll.setPrefViewportHeight(260);

		Label lbQuality = new Label(ResourceI18N.get(RES, "page.teenager.quality"));
		lbQuality.setWrapText(true);
		HBox bxButtons = new HBox(10, btnAdd, btnDel);
		bxButtons.setAlignment(Pos.CENTER_RIGHT);
		VBox bxQualities = new VBox(10, bxButtons, lvQualities);
		bxQualities.setStyle("-fx-pref-width: 20em; -fx-max-width: 420px; -fx-max-height: 14em");

		Label lbContact = new Label(ResourceI18N.get(RES, "page.teenager.contact"));
		lbContact.setWrapText(true);
		GridPane gpContact = new GridPane();
		gpContact.setHgap(10);
		gpContact.setVgap(5);
		gpContact.add(new Label(ResourceI18N.get(RES, "page.teenager.contact.type")), 0, 0);
		gpContact.add(cbContactType, 1, 0);
		gpContact.add(new Label(ResourceI18N.get(RES, "page.teenager.contact.name")), 0, 1);
		gpContact.add(tfContactName, 1, 1);
		gpContact.add(new Label(ResourceI18N.get(RES, "page.teenager.contact.influence")), 0, 2);
		gpContact.add(spInfluence, 1, 2);
		gpContact.add(new Label(ResourceI18N.get(RES, "page.teenager.contact.loyalty")), 0, 3);
		gpContact.add(lbLoyalty, 1, 3);

		Label lbSummarize = new Label(ResourceI18N.get(RES, "page.teenager.summarize"));
		VBox content = new VBox(20, lbSkill, skillScroll, lbAttribute, tpAttributes,
				lbQuality, bxQualities, lbContact, gpContact, new VBox(5, lbSummarize, taDescription));
		content.setPadding(new Insets(0, 0, 10, 0));
		ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		setContent(scroll);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		lvQualities.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> btnDel.setDisable(n==null));
		btnAdd.setOnAction(ev -> onAddQuality());
		btnDel.setOnAction(ev -> onDeleteQuality());

		cbContactType.valueProperty().addListener( (ov,o,n) -> {
			if (refreshing || n==null || earlyAdult==null) return;
			if (earlyAdult.getContact()==null) {
				earlyAdult.createContact(n);
			} else {
				earlyAdult.setContactType(n);
			}
		});
		tfContactName.textProperty().addListener( (ov,o,n) -> {
			if (refreshing || earlyAdult==null || earlyAdult.getContact()==null) return;
			earlyAdult.getContact().setName(n);
		});
		spInfluence.valueProperty().addListener( (ov,o,n) -> {
			if (refreshing || n==null || earlyAdult==null) return;
			earlyAdult.setContactInfluence(n);
		});
		taDescription.textProperty().addListener( (ov,o,n) -> {
			if (refreshing) return;
			charGen.getModel().setLifepathComingOfAgeBackground(n);
		});
	}

	//-------------------------------------------------------------------
	private void updateSkillButtons() {
		tpSkills.getChildren().clear();
		if (earlyAdult==null) return;
		SimpleSkillController ctrl = earlyAdult.getSkillController();
		List<SR6Skill> skills = earlyAdult.getAvailableSkills().stream()
				.sorted(Comparator.comparing(SR6Skill::getName))
				.toList();
		for (SR6Skill skill : skills) {
			ToggleButton btn = new ToggleButton(skill.getName());
			btn.setMaxWidth(Double.MAX_VALUE);
			btn.setSelected(ctrl.isSelected(skill));
			btn.setOnAction(ev -> {
				if (ctrl.isSelected(skill)) {
					ctrl.deselect(skill);
				} else {
					ctrl.select(skill);
				}
				refresh();
			});
			tpSkills.getChildren().add(btn);
		}
	}

	//-------------------------------------------------------------------
	private void updateAttributeButtons() {
		tpAttributes.getChildren().clear();
		if (earlyAdult==null) return;
		ShadowrunAttribute selected = charGen.getModel().getCharGenSettings(de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathSettings.class).getEarlyAdultAttribute();
		for (ShadowrunAttribute attribute : earlyAdult.getAvailableAttributes()) {
			ToggleButton btn = new ToggleButton(attribute.getName());
			btn.setMaxWidth(Double.MAX_VALUE);
			btn.setSelected(attribute==selected);
			btn.setOnAction(ev -> {
				if (attribute==charGen.getModel().getCharGenSettings(de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathSettings.class).getEarlyAdultAttribute()) {
					earlyAdult.deselectAttribute(attribute);
				} else {
					earlyAdult.selectAttribute(attribute);
				}
				refresh();
			});
			tpAttributes.getChildren().add(btn);
		}
	}

	//-------------------------------------------------------------------
	private void updateContact() {
		cbContactType.getItems().setAll(earlyAdult.getAvailableContactTypes());
		Contact contact = earlyAdult.getContact();
		tfContactName.setDisable(contact==null);
		spInfluence.setDisable(contact==null);
		if (contact==null) {
			cbContactType.setValue(null);
			lbLoyalty.setText("-");
			return;
		}
		cbContactType.setValue(contact.getType());
		if (!String.valueOf(contact.getName()).equals(tfContactName.getText()))
			tfContactName.setText(contact.getName());
		spInfluence.getValueFactory().setValue(contact.getRating());
		lbLoyalty.setText(String.valueOf(contact.getLoyalty()));
	}

	//-------------------------------------------------------------------
	protected void refresh() {
		boolean isLifepath = charGen.getId().equals("lifepath");
		activeProperty().set(isLifepath);
		if (!isLifepath || earlyAdult==null)
			return;
		refreshing = true;
		try {
			backHeader.setValue(charGen.getModel().getKarmaFree());
			updateSkillButtons();
			updateAttributeButtons();
			lvQualities.getItems().setAll(earlyAdult.getQualityController().getSelected());
			updateContact();
			String text = charGen.getModel().getLifepathComingOfAgeBackground();
			if (!(text!=null?text:"").equals(taDescription.getText()))
				taDescription.setText(text!=null?text:"");
		} finally {
			refreshing = false;
		}
	}

	//-------------------------------------------------------------------
	private void onAddQuality() {
		IQualityController ctrl = earlyAdult.getQualityController();
		QualitySelector selector = new SearchableQualitySelector(
				ctrl,
				Shadowrun6Tools.requirementResolver(Locale.getDefault()),
				Shadowrun6Tools.modificationResolver(Locale.getDefault())
				);
		ManagedDialog dialog = new ManagedDialog(ResourceI18N.get(RES,"section.quality.selector.title"), selector, CloseType.OK, CloseType.CANCEL);

		CloseType close = (CloseType) FlexibleApplication.getInstance().showAndWait(dialog);
		if (close!=CloseType.OK || selector.getSelected()==null)
			return;
		Quality toSelect = selector.getSelected();
		Possible possible = ctrl.canBeSelected(toSelect);
		if (!possible.get()) {
			logger.log(Level.DEBUG, "can not be Selected(" + toSelect + "): " + possible.getI18NKey());
			FlexibleApplication.getInstance().showAlertAndCall(AlertType.NOTIFICATION, "Selection failed", possible.toString());
			return;
		}
		OperationResult<QualityValue> res;
		List<Choice> options = ctrl.getChoicesToDecide(toSelect);
		if (!options.isEmpty()) {
			// User must decide first
			ChoiceSelectorDialog<Quality, QualityValue> choiceDialog = new ChoiceSelectorDialog<Quality, QualityValue>(ctrl);
			Decision[] decisions = choiceDialog.apply(toSelect, options);
			if (decisions == null)
				return;
			res = ctrl.select(toSelect, decisions);
		} else {
			res = ctrl.select(toSelect);
		}
		if (!res.wasSuccessful()) {
			logger.log(Level.WARNING, "Selecting {0} failed: {1}", toSelect, res.getError());
			AlertManager.showAlertAndCall(javafx.scene.control.Alert.AlertType.ERROR, "Failed adding", res.getError());
		}
		refresh();
	}

	//-------------------------------------------------------------------
	private void onDeleteQuality() {
		QualityValue val = lvQualities.getSelectionModel().getSelectedItem();
		if (val!=null)
			earlyAdult.getQualityController().deselect(val);
		refresh();
	}

	//-------------------------------------------------------------------
	@Override
	public void pageVisited() {
		refresh();
	}

	//-------------------------------------------------------------------
	@Override
	public void handleControllerEvent(ControllerEvent type, Object... param) {
		logger.log(Level.INFO, "RCV " + type + " with " + Arrays.toString(param));
		if (type == BasicControllerEvents.GENERATOR_CHANGED) {
			charGen = (SR6CharacterGenerator) param[0];
			earlyAdult = charGen.getEarlyAdultGenerator();
		}
		if (type == BasicControllerEvents.CHARACTER_CHANGED || type == BasicControllerEvents.GENERATOR_CHANGED) {
			refresh();
		}
	}
}
