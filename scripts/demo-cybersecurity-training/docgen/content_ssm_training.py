# -*- coding: utf-8 -*-
"""Self-Supervised Maintenance Guardian — training guide (EN + UK)."""

from __future__ import annotations

DATE = "2 July 2026"
DATE_UK = "2 липня 2026"


def training(lang: str) -> list:
    return _EN if lang == "en" else _UK


_META_EN = [
    ("Document", "Model Training Guide"),
    ("Product", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Trainer", "scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py"),
    ("Labels used", "0 (self-supervised)"),
    ("Output", "Deployable Jneopallium bundle (descriptor + context + layers)"),
    ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
    ("Date", DATE),
    ("License", "BSD 3-Clause"),
]

_EN = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Training Guide",
     "Fit the label-free parameters from telemetry and emit a deployable network — no fault history required",
     _META_EN, "Training Guide",
     "A copy-paste walkthrough of the training half: what the trainer fits, how to run it, how to bring "
     "your own historian data, how to verify the result, and exactly where training hands off to "
     "deployment. Python is used here for initial fitting only; the runtime model is Java."),

    ("toc", "Contents",
     ["What you will achieve",
      "Where training ends and deployment begins",
      "Prerequisites",
      "Step 1 — run the trainer",
      "Step 2 — what it fitted (the parameters)",
      "Step 3 — the bundle it produced",
      "Step 4 — bring your own data",
      "Step 5 — verify with the tests",
      "Step 6 — the hand-off to deployment",
      "Retraining and drift",
      "Troubleshooting",
      "Appendix — training cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end you will have a set of fitted, label-free parameters — per-regime standardisation, "
          "cross-sensor reconstruction weights, and health-baseline percentiles — written into a complete, "
          "deployable Jneopallium bundle that the Java runtime boots from directly. No failure labels are "
          "used at any point."),
    ("callout", "The safety ceiling applies to training too",
     "Nothing the trainer produces can actuate a device. The emitted model is advisory by construction "
     "(SsMaintConfig forbids disabling it), and severity is calibrated against healthy history — never "
     "against labelled failures, because there are none.", "warning"),

    ("h1", "Where training ends and deployment begins", "2"),
    ("table", ["Phase", "What happens", "Document"],
     [["**Training** (this guide)", "Fit standardisation, cross-sensor weights, baselines; emit the bundle",
       "Training Guide"],
      ["**Deployment**", "Load the bundle, wire telemetry + feedback, run the advisory scorer",
       "Deployment Guide"]],
     [1.9, 3.3, 1.6]),
    ("p", "The hand-off is one directory: worker/src/main/resources/model/self-supervised-maintenance/. "
          "Training **writes** it; deployment **reads** it."),

    ("h1", "Prerequisites", "3"),
    ("bullet", "Python 3.9+ (standard library only — no pip install needed)."),
    ("bullet", "A JDK 17 toolchain and Maven if you also want to run the Java runtime tests."),
    ("bullet", "The repository checked out; commands below are run from the repo root."),

    ("h1", "Step 1 — run the trainer", "4"),
    ("code", "cd scripts/demo-self-supervised-maintenance\n"
             "python train_ss_maintenance_model.py"),
    ("p", "Expected output (values are deterministic for the shipped synthetic corpus):"),
    ("code", "Self-Supervised Maintenance — initial (label-free) training\n\n"
             "  Trained on 6 assets x 2000 ticks, labels used: 0\n"
             "  Sensors                 : 8\n"
             "  Health baseline mean    : 0.7781\n"
             "  Health baseline p99     : 2.0808\n"
             "  Health baseline p999    : 2.7544\n"
             "  Neurons in bundle       : 4\n\n"
             "  Deployable bundle -> worker/src/main/resources/model/self-supervised-maintenance"),
    ("p", "`run_all.sh` (or `run_all.ps1`) runs the trainer and then the Python tests in one go."),

    ("h1", "Step 2 — what it fitted (the parameters)", "5"),
    ("p", "All three parameter groups are self-supervised — fitted from telemetry alone, on a "
          "trusted-healthy window:"),
    ("bullet", "**Per-regime standardisation** — the mean and standard deviation of every sensor in every "
               "operating regime, so a reading is judged relative to its load."),
    ("bullet", "**Cross-sensor reconstruction weights** — for each sensor, a ridge-regression model that "
               "predicts it from the other sensors. This is the label-free core: the target of each fit is "
               "another sensor, never a failure label."),
    ("bullet", "**Health-baseline percentiles** — the mean, p95, p99 and p999 of the reconstruction error "
               "over the healthy window, used to calibrate severity in the asset's own units."),
    ("callout", "Why ridge regression",
     "A small ridge (L2) penalty keeps the cross-sensor models stable when sensors are correlated — which, "
     "on a coupled skid, they always are. The solver is a plain normal-equation solve in pure Python; no "
     "third-party linear-algebra package is required.", "info"),

    ("h1", "Step 3 — the bundle it produced", "6"),
    ("table", ["File", "Purpose"],
     [["model-descriptor.json", "Manifest: layers, signals, data sources, labelFree=true, labelsUsed=0"],
      ["production-context.json", "IContext for the Entry runner (neuron classes, inputs, aggregator)"],
      ["layer-0-input.json", "Telemetry ingest descriptor + sensor order"],
      ["layer-1-selfsupervisedhealth.json", "Reconstruction neuron + fitted weights & standardisation"],
      ["layer-2-fusion.json", "Hypothesis neuron + baseline percentiles + fusion constants"],
      ["layer-3-onlinelearning.json", "Feedback-adaptation neuron + bounds/rate-limit/freeze"],
      ["layer-4-advisorygate.json", "Read-only gate + initial thresholds + dedup window"],
      ["fitted-model.json", "Raw fitted parameters for audit / reference"]],
     [2.9, 3.4]),
    ("p", "The layer files carry the parameters as neuron fields, so the files that record your fit are the "
          "files the runtime binds. This mirrors the industrial model's bundle layout."),

    ("h1", "Step 4 — bring your own data", "7"),
    ("p", "For a real site, replace the synthetic generator with your historian export. The trainer reads a "
          "list of per-tick rows through `synth_telemetry.generate`; point it at your data instead. Each "
          "canonical row must have:"),
    ("bullet", "The same **sensor names** the model expects (or edit `SENSORS` to match your tag set)."),
    ("bullet", "An integer **regime** column (operating point / load band). If you don't have one, derive "
               "it from a load or power band."),
    ("bullet", "**No labels.** The training window should be **mostly healthy** — this is the one "
               "assumption that matters. If an asset was already faulty during the window, exclude that "
               "period so it does not poison 'normal.'"),
    ("callout", "Fleet baselines for cold starts",
     "If an asset has too little history to define its own normal, fit against sibling assets in the same "
     "regime. The model reports domain-shift and uncertainty so an unfamiliar asset reads as 'watching' "
     "rather than making confident claims.", "info"),

    ("h1", "Step 5 — verify with the tests", "8"),
    ("code", "cd scripts/demo-self-supervised-maintenance\n"
             "python -m unittest tests.test_ss_maintenance -v"),
    ("p", "The Python tests confirm the trainer produced a sound model:"),
    ("bullet", "baseline percentiles are correctly ordered (mean < p95 < p99 < p999);"),
    ("bullet", "all cross-sensor weights are finite;"),
    ("bullet", "healthy frames rarely exceed the asset's own p99 (nuisance rate under 5%);"),
    ("bullet", "**every injected fault family is separated from the healthy baseline without labels** "
               "(reconstruction error above 2× p999), and the dominant residual points at the right "
               "sensor group;"),
    ("bullet", "the bundle is written and valid (labelFree=true, labelsUsed=0, safetyMode=ADVISORY)."),
    ("p", "The runtime behaviour — evidence accumulation, feedback adaptation, the gate — is validated "
          "separately by the Java suite `SelfSupervisedMaintenanceModuleTest` (14 tests). Run it with "
          "`mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test`."),

    ("h1", "Step 6 — the hand-off to deployment", "9"),
    ("p", "Training is done when the bundle directory is written and the tests pass. The Deployment Guide "
          "takes it from there: it loads the same bundle with the Entry runner, wires your OPC UA / MQTT "
          "telemetry to AssetTelemetrySignal and your operator feedback to OperatorFeedbackSignal, and "
          "starts producing advisories."),

    ("h1", "Retraining and drift", "10"),
    ("p", "Refit the baselines when the plant genuinely changes (new product, rebuild, seasonal shift) — "
          "not on every blip. Day-to-day adaptation is handled at runtime by the feedback loop, which needs "
          "no retraining and no redeploy. A practical cadence: refit the standardisation and baselines on a "
          "rolling healthy window monthly or after a known change, and let the online loop handle the rest."),

    ("h1", "Troubleshooting", "11"),
    ("table", ["Symptom", "Likely cause", "Fix"],
     [["Healthy assets flag often", "Training window wasn't healthy", "Exclude faulty periods; refit"],
      ["A fault isn't separated", "Sensor coupling too weak / missing channel", "Add the coupled channel"],
      ["Everything reads uncertain", "Data unlike training (domain shift)", "Refit on representative data"],
      ["Percentiles look wrong", "Too few ticks per regime", "Give each regime enough samples"]],
     [1.9, 2.4, 2.4]),

    ("h1", "Appendix — training cheat-sheet", "12"),
    ("code", "# fit + emit bundle\n"
             "python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py\n\n"
             "# python tests (trainer + label-free separation)\n"
             "python -m unittest tests.test_ss_maintenance\n\n"
             "# java runtime tests\n"
             "mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test"),
    ("pi", "Training here means fitting 'normal' from telemetry and emitting a deployable network — with "
           "zero labels. Everything about a specific failure is learned later, at runtime, from operator "
           "feedback."),
]


_META_UK = [
    ("Документ", "Посібник з навчання моделі"),
    ("Продукт", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Тренер", "scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py"),
    ("Використано міток", "0 (самокероване)"),
    ("Вихід", "Розгортувана збірка Jneopallium (дескриптор + контекст + шари)"),
    ("Автор", "Дмитро Раковський — Харків, Україна"),
    ("Дата", DATE_UK),
    ("Ліцензія", "BSD 3-Clause"),
]

_UK = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Посібник з навчання",
     "Припасуйте параметри без міток із телеметрії та згенеруйте розгортувану мережу — історія відмов не "
     "потрібна",
     _META_UK, "Посібник з навчання",
     "Покроковий огляд навчальної частини: що припасовує тренер, як його запустити, як підключити власні "
     "дані історіана, як перевірити результат і де саме навчання передає естафету розгортанню. Python тут "
     "лише для початкового припасування; середовище виконання — Java."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Де закінчується навчання і починається розгортання",
      "Передумови",
      "Крок 1 — запустити тренер",
      "Крок 2 — що він припасував (параметри)",
      "Крок 3 — збірка, яку він створив",
      "Крок 4 — підключити власні дані",
      "Крок 5 — перевірити тестами",
      "Крок 6 — передача до розгортання",
      "Перенавчання та дрейф",
      "Усунення несправностей",
      "Додаток — шпаргалка навчання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "Наприкінці ви матимете набір припасованих параметрів без міток — стандартизацію за режимами, ваги "
          "міжсенсорного відновлення та перцентилі базового рівня здоров'я — записаних у повну розгортувану "
          "збірку Jneopallium, з якої середовище Java завантажується напряму. Мітки відмов не "
          "використовуються на жодному етапі."),
    ("callout", "Стеля безпеки стосується й навчання",
     "Ніщо, що створює тренер, не може керувати пристроєм. Отримана модель є рекомендаційною за побудовою "
     "(SsMaintConfig забороняє це вимкнути), а тяжкість калібрується за здоровою історією — ніколи за "
     "розміченими відмовами, бо їх немає.", "warning"),

    ("h1", "Де закінчується навчання і починається розгортання", "2"),
    ("table", ["Фаза", "Що відбувається", "Документ"],
     [["**Навчання** (цей посібник)", "Припасувати стандартизацію, ваги, базові рівні; згенерувати збірку",
       "Посібник з навчання"],
      ["**Розгортання**", "Завантажити збірку, підключити телеметрію + зворотний зв'язок, запустити скорер",
       "Посібник з розгортання"]],
     [1.9, 3.3, 1.6]),
    ("p", "Естафета — це один каталог: worker/src/main/resources/model/self-supervised-maintenance/. "
          "Навчання **записує** його; розгортання **читає** його."),

    ("h1", "Передумови", "3"),
    ("bullet", "Python 3.9+ (лише стандартна бібліотека — pip install не потрібен)."),
    ("bullet", "JDK 17 і Maven, якщо ви також хочете запускати Java-тести середовища виконання."),
    ("bullet", "Репозиторій клоновано; команди нижче виконуються з кореня репозиторію."),

    ("h1", "Крок 1 — запустити тренер", "4"),
    ("code", "cd scripts/demo-self-supervised-maintenance\n"
             "python train_ss_maintenance_model.py"),
    ("p", "Очікуваний вивід (значення детерміновані для наявного синтетичного корпусу):"),
    ("code", "Self-Supervised Maintenance — initial (label-free) training\n\n"
             "  Trained on 6 assets x 2000 ticks, labels used: 0\n"
             "  Sensors                 : 8\n"
             "  Health baseline mean    : 0.7781\n"
             "  Health baseline p99     : 2.0808\n"
             "  Health baseline p999    : 2.7544\n"
             "  Neurons in bundle       : 4\n\n"
             "  Deployable bundle -> worker/src/main/resources/model/self-supervised-maintenance"),
    ("p", "`run_all.sh` (або `run_all.ps1`) запускає тренер, а потім Python-тести за один прохід."),

    ("h1", "Крок 2 — що він припасував (параметри)", "5"),
    ("p", "Усі три групи параметрів є самокерованими — припасованими лише з телеметрії, на довіреному "
          "здоровому вікні:"),
    ("bullet", "**Стандартизація за режимами** — середнє й стандартне відхилення кожного сенсора в кожному "
               "режимі, щоб оцінювати зчитування відносно навантаження."),
    ("bullet", "**Ваги міжсенсорного відновлення** — для кожного сенсора модель гребеневої регресії, що "
               "передбачає його за іншими сенсорами. Це ядро без міток: ціль кожного припасування — інший "
               "сенсор, ніколи не мітка відмови."),
    ("bullet", "**Перцентилі базового рівня здоров'я** — середнє, p95, p99 і p999 помилки відновлення на "
               "здоровому вікні, для калібрування тяжкості у власних одиницях активу."),
    ("callout", "Чому гребенева регресія",
     "Невеликий гребеневий (L2) штраф тримає міжсенсорні моделі стабільними, коли сенсори корельовані — а "
     "на пов'язаній установці вони завжди такі. Розв'язувач — звичайне розв'язання нормальних рівнянь на "
     "чистому Python; сторонній пакет лінійної алгебри не потрібен.", "info"),

    ("h1", "Крок 3 — збірка, яку він створив", "6"),
    ("table", ["Файл", "Призначення"],
     [["model-descriptor.json", "Маніфест: шари, сигнали, джерела даних, labelFree=true, labelsUsed=0"],
      ["production-context.json", "IContext для запускача Entry (класи нейронів, входи, агрегатор)"],
      ["layer-0-input.json", "Дескриптор прийому телеметрії + порядок сенсорів"],
      ["layer-1-selfsupervisedhealth.json", "Нейрон відновлення + припасовані ваги та стандартизація"],
      ["layer-2-fusion.json", "Нейрон гіпотези + базові перцентилі + константи поєднання"],
      ["layer-3-onlinelearning.json", "Нейрон адаптації + межі/обмеження частоти/заморожування"],
      ["layer-4-advisorygate.json", "Шлюз лише для читання + початкові пороги + вікно дедуплікації"],
      ["fitted-model.json", "Сирі припасовані параметри для аудиту / довідки"]],
     [2.9, 3.4]),
    ("p", "Файли шарів несуть параметри як поля нейронів, тож файли, що фіксують ваше припасування, — це "
          "файли, які прив'язує середовище виконання. Це віддзеркалює компонування збірки промислової "
          "моделі."),

    ("h1", "Крок 4 — підключити власні дані", "7"),
    ("p", "Для реального майданчика замініть синтетичний генератор експортом свого історіана. Тренер читає "
          "список рядків по тактах через `synth_telemetry.generate`; спрямуйте його на свої дані. Кожен "
          "канонічний рядок має містити:"),
    ("bullet", "Ті самі **назви сенсорів**, яких очікує модель (або відредагуйте `SENSORS` під свій набір "
               "тегів)."),
    ("bullet", "Цілочисельний стовпець **режиму** (робоча точка / смуга навантаження). Якщо його немає — "
               "виведіть його зі смуги навантаження чи потужності."),
    ("bullet", "**Без міток.** Вікно навчання має бути **переважно здоровим** — це єдине важливе "
               "припущення. Якщо актив уже був несправним у цьому вікні, виключіть цей період, щоб він не "
               "отруїв «норму»."),
    ("callout", "Базові рівні парку для холодного старту",
     "Якщо в активу замало історії, щоб визначити власну норму, припасовуйте за спорідненими активами того "
     "ж режиму. Модель повідомляє зсув домену й невизначеність, тож незнайомий актив читається як "
     "«спостерігаю», а не робить упевнені заяви.", "info"),

    ("h1", "Крок 5 — перевірити тестами", "8"),
    ("code", "cd scripts/demo-self-supervised-maintenance\n"
             "python -m unittest tests.test_ss_maintenance -v"),
    ("p", "Python-тести підтверджують, що тренер створив надійну модель:"),
    ("bullet", "перцентилі базового рівня правильно впорядковані (mean < p95 < p99 < p999);"),
    ("bullet", "усі ваги міжсенсорного відновлення скінченні;"),
    ("bullet", "здорові кадри рідко перевищують власний p99 активу (частка зайвих спрацювань нижче 5%);"),
    ("bullet", "**кожне впроваджене сімейство несправностей відокремлюється від здорового базового рівня "
               "без міток** (помилка відновлення понад 2× p999), і домінантний залишок вказує на правильну "
               "групу сенсорів;"),
    ("bullet", "збірка записана й валідна (labelFree=true, labelsUsed=0, safetyMode=ADVISORY)."),
    ("p", "Поведінку середовища виконання — накопичення свідчень, адаптацію зворотного зв'язку, шлюз — "
          "перевіряє окремо Java-набір `SelfSupervisedMaintenanceModuleTest` (14 тестів). Запустіть його "
          "через `mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test`."),

    ("h1", "Крок 6 — передача до розгортання", "9"),
    ("p", "Навчання завершене, коли каталог збірки записаний і тести проходять. Далі за справу береться "
          "посібник з розгортання: він завантажує ту саму збірку запускачем Entry, підключає вашу "
          "телеметрію OPC UA / MQTT до AssetTelemetrySignal, а зворотний зв'язок операторів до "
          "OperatorFeedbackSignal, і починає видавати рекомендації."),

    ("h1", "Перенавчання та дрейф", "10"),
    ("p", "Перепасовуйте базові рівні, коли установка справді змінюється (новий продукт, капремонт, "
          "сезонний зсув) — а не на кожному сплеску. Щоденну адаптацію виконує під час роботи цикл "
          "зворотного зв'язку, якому не потрібні перенавчання чи переустановлення. Практичний ритм: "
          "перепасовувати стандартизацію й базові рівні на ковзному здоровому вікні щомісяця або після "
          "відомої зміни, а решту хай робить онлайн-цикл."),

    ("h1", "Усунення несправностей", "11"),
    ("table", ["Симптом", "Ймовірна причина", "Виправлення"],
     [["Здорові активи часто сигналять", "Вікно навчання не було здоровим", "Виключити несправні періоди"],
      ["Несправність не відокремлюється", "Слабкий зв'язок / відсутній канал", "Додати пов'язаний канал"],
      ["Усе читається як невизначене", "Дані не схожі на навчальні (зсув)", "Перепасувати на типових даних"],
      ["Перцентилі виглядають хибними", "Замало тактів на режим", "Дати кожному режиму досить вибірки"]],
     [1.9, 2.4, 2.4]),

    ("h1", "Додаток — шпаргалка навчання", "12"),
    ("code", "# припасувати + згенерувати збірку\n"
             "python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py\n\n"
             "# python-тести (тренер + відокремлення без міток)\n"
             "python -m unittest tests.test_ss_maintenance\n\n"
             "# java-тести середовища виконання\n"
             "mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test"),
    ("pi", "Навчання тут означає припасування «норми» з телеметрії та генерацію розгортуваної мережі — з "
           "нульом міток. Усе про конкретну відмову вивчається пізніше, під час роботи, зі зворотного "
           "зв'язку операторів."),
]
