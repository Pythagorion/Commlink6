package de.rpgframework.shadowrun6.chargen.jfx.wizard;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.DataItem;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.genericrpg.requirements.Requirement;
import de.rpgframework.jfx.ComplexDataItemControllerNode;
import de.rpgframework.jfx.ComplexDataItemListFilter;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.jfx.wizard.NumberUnitBackHeader;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun6.CreatePoints;
import de.rpgframework.shadowrun6.LifepathModule;
import de.rpgframework.shadowrun6.LifepathModule.Type;
import de.rpgframework.shadowrun6.LifepathModuleValue;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterGenerator;
import de.rpgframework.shadowrun6.chargen.gen.GeneratorWrapper;
import de.rpgframework.shadowrun6.chargen.gen.lifepath.SR6LifePathModuleGenerator;
import de.rpgframework.shadowrun6.chargen.jfx.listcell.LifepathModuleListCell;
import de.rpgframework.shadowrun6.chargen.jfx.listcell.LifepathModuleValueListCell;
import de.rpgframework.shadowrun6.chargen.jfx.pane.LifepathModuleDescriptionPane;
import de.rpgframework.shadowrun6.chargen.jfx.selector.ChoiceSelectorDialog;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * @author prelle
 */
public class SR6WizardPageLPAdult extends WizardPage implements ControllerListener {

	private final static Logger logger = System.getLogger(SR6WizardPageLPAdult.class.getPackageName());
	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6WizardPageLPAdult.class.getPackageName()+".SR6WizardPages");

	protected SR6CharacterGenerator charGen;
	protected SR6LifePathModuleGenerator modules;
	protected ComplexDataItemControllerNode<LifepathModule, LifepathModuleValue> selection;
	protected GenericDescriptionVBox bxDescription;
	protected OptionalNodePane layout;
	private VBox content;
	private NumberUnitBackHeader backHeader;
	private Label lbIntro;
	private Label lbCount;
	private TextArea taDescription;

	//-------------------------------------------------------------------
	public SR6WizardPageLPAdult(Wizard wizard, GeneratorWrapper charGen) {
		super(wizard);
		this.charGen = charGen;
		this.modules = charGen.getWrapped().getLifePathModuleGenerator();
		setTitle(ResourceI18N.get(RES, "page.adult.title"));
		initComponents();
		initLayout();
		initInteractivity();
		charGen.addListener(this);
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		bxDescription = new LifepathModuleDescriptionPane(Shadowrun6Tools.requirementResolver(Locale.getDefault()), Shadowrun6Tools.modificationResolver(Locale.getDefault()));
		lbCount = new Label();
		ensureSelection();
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		backHeader = new NumberUnitBackHeader("Karma");
		backHeader.setValue(charGen.getModel().getKarmaFree());
		HBox.setMargin(backHeader, new Insets(0,10,0,10));
		super.setBackHeader(backHeader);

		lbIntro = new Label();
		lbIntro.setWrapText(true);
		content = new VBox(10, lbIntro, lbCount);
		if (selection!=null)
			content.getChildren().add(selection);
		// Free text below the module selection, which may be added to content later
		taDescription = new TextArea();
		taDescription.setWrapText(true);
		taDescription.setMaxWidth(450);
		taDescription.setPrefRowCount(4);
		taDescription.setPromptText(ResourceI18N.get(RES, "page.adult.prompt"));
		taDescription.textProperty().addListener( (ov,o,n) -> charGen.getModel().setLifepathAdultBackground(n));
		VBox bxFluff = new VBox(5, new Label(ResourceI18N.get(RES, "page.adult.summarize")), taDescription);
		VBox page = new VBox(10, content, bxFluff);
		VBox.setVgrow(content, Priority.ALWAYS);
		layout = new OptionalNodePane(page, bxDescription);
		setContent(layout);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		if (selection!=null)
			initSelectionInteractivity();
	}

	//-------------------------------------------------------------------
	private void ensureSelection() {
		if (selection!=null || modules==null)
			return;
		selection = new ComplexDataItemControllerNode<>(modules);
		selection.setAvailableStyle("-fx-min-width: 20em; -fx-max-width: 28em");
		selection.setSelectedStyle("-fx-min-width: 20em; -fx-max-width: 30em");
		selection.setAvailablePlaceholder(ResourceI18N.get(RES, "page.adult.placeholder.available"));
		selection.setSelectedPlaceholder(ResourceI18N.format(RES, "page.adult.placeholder.selected", modules.getMaximumModules()));
		selection.setShowHeadings(ResponsiveControlManager.getCurrentMode()!=WindowMode.MINIMAL);
		selection.setRequirementResolver(Shadowrun6Tools.requirementResolver(Locale.getDefault()));
		selection.setModificationResolver(Shadowrun6Tools.modificationResolver(Locale.getDefault()));
		selection.setAvailableCellFactory(lv -> new LifepathModuleListCell(() -> selection.getController(), Shadowrun6Tools.requirementResolver(Locale.getDefault())));
		selection.setSelectedCellFactory(lv -> new LifepathModuleValueListCell(() -> selection.getController()));
		selection.setOptionCallback(new ChoiceSelectorDialog<>(modules));
		selection.setFilterNode(new LifepathModuleFilter(selection, modules, Shadowrun6Tools.requirementResolver(Locale.getDefault()), Shadowrun6Tools.modificationResolver(Locale.getDefault())));
		initSelectionInteractivity();
		if (content!=null && !content.getChildren().contains(selection))
			content.getChildren().add(selection);
	}

	//-------------------------------------------------------------------
	private void initSelectionInteractivity() {
		selection.showHelpForProperty().addListener((ov, o, n) -> {
			bxDescription.setData(n);
			layout.setTitle(n!=null?n.getName():null);
		});
	}

	//-------------------------------------------------------------------
	protected void refresh() {
		boolean isLifepath = charGen.getId().equals("lifepath");
		activeProperty().set(isLifepath);
		if (!isLifepath || modules==null)
			return;
		ensureSelection();
		backHeader.setValue(charGen.getModel().getKarmaFree());
		int maximumModules = modules.getMaximumModules();
		lbIntro.setText(ResourceI18N.format(RES, "page.adult.intro", maximumModules));
		lbCount.setText(ResourceI18N.format(RES, "page.adult.count", modules.getSelected().size(), maximumModules));
		String text = charGen.getModel().getLifepathAdultBackground();
		if (!(text!=null?text:"").equals(taDescription.getText()))
			taDescription.setText(text!=null?text:"");
		if (selection!=null) {
			selection.setSelectedPlaceholder(ResourceI18N.format(RES, "page.adult.placeholder.selected", maximumModules));
			selection.refresh();
		}
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
			modules = charGen.getLifePathModuleGenerator();
			if (modules!=null) {
				if (selection==null) {
					ensureSelection();
				} else {
					selection.setController(modules);
					selection.setOptionCallback(new ChoiceSelectorDialog<>(modules));
					selection.setFilterNode(new LifepathModuleFilter(selection, modules, Shadowrun6Tools.requirementResolver(Locale.getDefault()), Shadowrun6Tools.modificationResolver(Locale.getDefault())));
				}
			}
		}
		if (type == BasicControllerEvents.CHARACTER_CHANGED || type == BasicControllerEvents.GENERATOR_CHANGED) {
			refresh();
		}
	}

	//-------------------------------------------------------------------
	private static class LifepathModuleFilter extends ComplexDataItemListFilter<LifepathModule, LifepathModuleValue> {

		private enum SortOrder {
			AZ,
			ZA
		}

		private final SR6LifePathModuleGenerator modules;
		private final Function<Requirement, String> requirementResolver;
		private final Function<Modification, String> modificationResolver;
		private final Collator collator;

		private TextField tfSearch;
		private ComboBox<SortOrder> cbSort;
		private CheckBox cbOnlyMet;
		private CheckBox cbHasPrerequisites;
		private CheckBox cbEvent;
		private CheckBox cbMoney;
		private CheckBox cbQuality;
		private CheckBox cbSkill;
		private CheckBox cbAttribute;

		//-------------------------------------------------------------------
		public LifepathModuleFilter(ComplexDataItemControllerNode<LifepathModule, LifepathModuleValue> parent,
				SR6LifePathModuleGenerator modules,
				Function<Requirement, String> requirementResolver,
				Function<Modification, String> modificationResolver) {
			super(parent);
			this.modules = modules;
			this.requirementResolver = requirementResolver;
			this.modificationResolver = modificationResolver;
			this.collator = Collator.getInstance(Locale.getDefault());
			initComponents();
			initLayout();
			initInteractivity();
		}

		//-------------------------------------------------------------------
		private void initComponents() {
			tfSearch = new TextField();
			tfSearch.setPromptText(ResourceI18N.get(RES, "page.adult.filter.search"));

			cbSort = new ComboBox<>(FXCollections.observableArrayList(SortOrder.AZ, SortOrder.ZA));
			cbSort.setConverter(new StringConverter<SortOrder>() {
				@Override
				public String toString(SortOrder value) {
					if (value==null) return "";
					return ResourceI18N.get(RES, "page.adult.filter.sort."+value.name());
				}
				@Override
				public SortOrder fromString(String string) {
					return null;
				}
			});
			cbSort.setValue(SortOrder.AZ);

			cbOnlyMet = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.onlyMet"));
			cbHasPrerequisites = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.hasPrerequisites"));
			cbEvent = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.event"));
			cbMoney = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.money"));
			cbQuality = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.quality"));
			cbSkill = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.skill"));
			cbAttribute = new CheckBox(ResourceI18N.get(RES, "page.adult.filter.attribute"));
		}

		//-------------------------------------------------------------------
		private void initLayout() {
			VBox filterContent = new VBox(6,
					tfSearch,
					cbSort,
					new Separator(),
					cbOnlyMet,
					cbHasPrerequisites,
					cbEvent,
					new Separator(),
					cbMoney,
					cbQuality,
					cbSkill,
					cbAttribute);
			filterContent.setPadding(new Insets(6));

			TitledPane pane = new TitledPane(ResourceI18N.get(RES, "page.adult.filter.title"), filterContent);
			pane.setExpanded(false);

			Accordion accordion = new Accordion(pane);
			accordion.setExpandedPane(null);
			VBox.setMargin(accordion, new Insets(0, 0, 8, 0));

			getChildren().add(accordion);
		}

		//-------------------------------------------------------------------
		private void initInteractivity() {
			tfSearch.textProperty().addListener((ov,o,n) -> applyFilter());
			cbSort.valueProperty().addListener((ov,o,n) -> applyFilter());
			List<CheckBox> boxes = List.of(cbOnlyMet, cbHasPrerequisites, cbEvent, cbMoney, cbQuality, cbSkill, cbAttribute);
			boxes.forEach(box -> box.selectedProperty().addListener((ov,o,n) -> applyFilter()));
		}

		//-------------------------------------------------------------------
		@Override
		public void applyFilter() {
			List<LifepathModule> filtered = modules.getAvailable().stream()
					.filter(this::matchesFilters)
					.sorted(getComparator())
					.collect(Collectors.toList());
			parent.getAvailable().setAll(filtered);
		}

		//-------------------------------------------------------------------
		private Comparator<LifepathModule> getComparator() {
			Comparator<LifepathModule> comparator = (a,b) -> collator.compare(a.getName(Locale.getDefault()), b.getName(Locale.getDefault()));
			if (cbSort.getValue()==SortOrder.ZA)
				comparator = comparator.reversed();
			return comparator;
		}

		//-------------------------------------------------------------------
		private boolean matchesFilters(LifepathModule module) {
			if (!matchesSearch(module))
				return false;
			if (cbOnlyMet.isSelected() && !requirementsMet(module))
				return false;
			if (cbHasPrerequisites.isSelected() && module.getRequirements().isEmpty())
				return false;
			if (cbEvent.isSelected() && module.getType()!=Type.EVENT)
				return false;
			if (cbMoney.isSelected() && !hasModification(module, this::isMoneyModification))
				return false;
			if (cbQuality.isSelected() && !hasModification(module, this::isQualityModification))
				return false;
			if (cbSkill.isSelected() && !hasModification(module, this::isSkillModification))
				return false;
			if (cbAttribute.isSelected() && !hasModification(module, this::isAttributeModification))
				return false;
			return true;
		}

		//-------------------------------------------------------------------
		private boolean matchesSearch(LifepathModule module) {
			String query = normalize(tfSearch.getText());
			if (query.isBlank())
				return true;

			List<String> searchable = new ArrayList<>();
			searchable.add(module.getName(Locale.getDefault()));
			searchable.add(module.getId());
			if (module.getType()!=null)
				searchable.add(ResourceI18N.get(RES, "page.adult.module.type."+module.getType().name()));
			module.getRequirements().stream()
				.map(req -> requirementResolver!=null?requirementResolver.apply(req):String.valueOf(req))
				.forEach(searchable::add);
			module.getChoices().forEach(choice -> addChoiceSearchTerms(searchable, choice));
			forEachModification(module, mod -> addModificationSearchTerms(searchable, mod));

			return searchable.stream()
					.filter(text -> text!=null)
					.map(this::normalize)
					.anyMatch(text -> text.contains(query));
		}

		//-------------------------------------------------------------------
		private void addChoiceSearchTerms(List<String> searchable, Choice choice) {
			if (choice==null || choice.getChooseFrom()==null)
				return;
			searchable.add(String.valueOf(choice.getChooseFrom()));
			if (choice.getChoiceOptions()!=null) {
				for (String option : choice.getChoiceOptions()) {
					addReferenceSearchTerm(searchable, choice.getChooseFrom(), option);
				}
			}
		}

		//-------------------------------------------------------------------
		private void addModificationSearchTerms(List<String> searchable, Modification mod) {
			if (modificationResolver!=null)
				searchable.add(modificationResolver.apply(mod));
			if (mod instanceof DataItemModification dataMod) {
				searchable.add(String.valueOf(dataMod.getReferenceType()));
				for (String key : dataMod.getAsKeys()) {
					addReferenceSearchTerm(searchable, dataMod.getReferenceType(), key.trim());
				}
			}
		}

		//-------------------------------------------------------------------
		private void addReferenceSearchTerm(List<String> searchable, Object referenceType, String key) {
			if (key==null || key.isBlank())
				return;
			searchable.add(key);
			try {
				if (referenceType==ShadowrunReference.ATTRIBUTE) {
					searchable.add(ShadowrunAttribute.valueOf(key).getName(Locale.getDefault()));
				} else if (referenceType==ShadowrunReference.SKILL || referenceType==ShadowrunReference.SKILL_KNOWLEDGE) {
					DataItem skill = Shadowrun6Core.getSkill(key);
					if (skill!=null)
						searchable.add(skill.getName(Locale.getDefault()));
				} else if (referenceType instanceof ShadowrunReference ref && !"CHOICE".equals(key)) {
					Object resolved = ref.resolve(key);
					if (resolved instanceof DataItem item)
						searchable.add(item.getName(Locale.getDefault()));
					else if (resolved!=null)
						searchable.add(String.valueOf(resolved));
				}
			} catch (Exception e) {
				logger.log(Level.DEBUG, "Cannot resolve life module search term {0}:{1}", referenceType, key);
			}
		}

		//-------------------------------------------------------------------
		private boolean requirementsMet(LifepathModule module) {
			Possible possible = Shadowrun6Tools.checkDecisionsAndRequirements(modules.getModel(), module);
			return possible.get();
		}

		//-------------------------------------------------------------------
		private boolean hasModification(LifepathModule module, Function<Modification, Boolean> predicate) {
			final boolean[] found = new boolean[] { false };
			forEachModification(module, mod -> {
				if (predicate.apply(mod))
					found[0] = true;
			});
			return found[0];
		}

		//-------------------------------------------------------------------
		private void forEachModification(LifepathModule module, java.util.function.Consumer<Modification> consumer) {
			for (Modification mod : module.getOutgoingModifications()) {
				if (mod instanceof ModificationChoice choice) {
					choice.getModificiations().forEach(consumer);
				} else {
					consumer.accept(mod);
				}
			}
		}

		//-------------------------------------------------------------------
		private boolean isMoneyModification(Modification mod) {
			if (mod instanceof DataItemModification dataMod && dataMod.getReferenceType()==ShadowrunReference.CREATION_POINTS) {
				for (String key : dataMod.getAsKeys()) {
					if (CreatePoints.NUYEN.name().equals(key.trim()) || "RESOURCES".equals(key.trim()))
						return true;
				}
			}
			return false;
		}

		//-------------------------------------------------------------------
		private boolean isQualityModification(Modification mod) {
			return mod instanceof DataItemModification dataMod && dataMod.getReferenceType()==ShadowrunReference.QUALITY;
		}

		//-------------------------------------------------------------------
		private boolean isSkillModification(Modification mod) {
			return mod instanceof ValueModification dataMod && dataMod.getReferenceType()==ShadowrunReference.SKILL;
		}

		//-------------------------------------------------------------------
		private boolean isAttributeModification(Modification mod) {
			return mod instanceof ValueModification dataMod && dataMod.getReferenceType()==ShadowrunReference.ATTRIBUTE;
		}

		//-------------------------------------------------------------------
		private String normalize(String value) {
			return value==null?"":value.toLowerCase(Locale.getDefault()).trim();
		}
	}
}
