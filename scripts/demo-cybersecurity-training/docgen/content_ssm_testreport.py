# -*- coding: utf-8 -*-
"""Self-Supervised Maintenance Guardian — test report (EN + UK)."""

from __future__ import annotations

DATE = "2 July 2026"
DATE_UK = "2 липня 2026"


def testreport(lang: str) -> list:
    return _EN if lang == "en" else _UK


_META_EN = [
    ("Document", "Test Report"),
    ("Product", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Java suite", "SelfSupervisedMaintenanceModuleTest — 14/14 passing"),
    ("Python suite", "tests/test_ss_maintenance.py — 5/5 passing"),
    ("Method", "Deterministic; fault labels used only to score, never to train"),
    ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
    ("Date", DATE),
    ("License", "BSD 3-Clause"),
]

_EN = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Test Report",
     "What was tested, what passed, and what the results mean — including two real bugs the tests caught",
     _META_EN, "Test Report",
     "A plain-language account of the verification: the Java runtime suite, the Python training suite, the "
     "label-free separation results, the two latent bugs the tests surfaced and their fixes, and an honest "
     "statement of what these results do and do not prove."),

    ("toc", "Contents",
     ["Summary",
      "For the non-specialist: how we tested without labels",
      "What was under test",
      "Java runtime suite (14 tests)",
      "Python training suite (5 tests)",
      "Label-free separation results",
      "Two bugs the tests caught",
      "Determinism and reproducibility",
      "What these results prove — and what they don't",
      "How to reproduce"]),

    ("h1", "Summary", "1"),
    ("table", ["Suite", "Tests", "Result"],
     [["Java runtime (SelfSupervisedMaintenanceModuleTest)", "14", "all passing"],
      ["Python training (test_ss_maintenance)", "5", "all passing"],
      ["Latent bugs found and fixed", "2", "overflow in dedup & rate-limit"]],
     [3.4, 1.2, 1.7]),
    ("callout", "Headline",
     "Every injected fault family is detected without any label, healthy assets stay quiet, the feedback "
     "loop moves thresholds the right way, and the model provably cannot actuate. Two real timestamp-"
     "overflow bugs were found by the tests and fixed.", "success"),

    ("h1", "For the non-specialist: how we tested without labels", "2"),
    ("p", "A supervised model is easy to test: you hide some labelled failures and check it finds them. "
          "This model has no labels, so the test is different. We generate realistic telemetry, secretly "
          "inject a slow degradation into some assets, and keep the timing of that degradation in a sealed "
          "'answer key' the model never sees. The model is trained only on healthy data. Then we check: did "
          "it flag the degrading assets (using the answer key only to score), and did it leave the healthy "
          "ones alone? That is a fair test of a label-free detector."),

    ("h1", "What was under test", "3"),
    ("bullet", "**Runtime (Java):** the four neurons, six signals, and five processors — the model that "
               "actually runs in production."),
    ("bullet", "**Training (Python):** the label-free fit and the deployable bundle it emits."),
    ("bullet", "**Safety invariants:** that the model is advisory and cannot be switched to actuate."),

    ("h1", "Java runtime suite (14 tests)", "4"),
    ("p", "Run through the real JUnit platform. Coverage by area:"),
    ("table", ["Area", "What is asserted"],
     [["Reconstruction", "healthy frame → low residual; anomaly → high residual on the right sensor; "
                         "domain-shift flagged"],
      ["Hypothesis", "a rising residual accumulates evidence, elevates severity, attributes the family, "
                     "and yields a lead time; a healthy stream stays quiet"],
      ["Feedback", "false positive raises the threshold, confirmed relaxes it, adaptation freezes during "
                   "domain shift, first update is not suppressed"],
      ["Advisory gate", "gates on threshold, de-duplicates, applies live threshold updates, first advisory "
                        "not suppressed, advisoryOnly true"],
      ["Config / processors", "advisory-only invariant holds; all processors are interface-typed"]],
     [1.7, 4.6]),

    ("h1", "Python training suite (5 tests)", "5"),
    ("table", ["Test", "What it confirms"],
     [["baseline percentiles ordered", "mean < p95 < p99 < p999"],
      ["cross weights finite", "every fitted weight is a finite number"],
      ["healthy frames low residual", "a healthy asset exceeds its own p99 under 5% of the time"],
      ["faults separate without labels", "each fault family sits above 2× p999 and points at the right "
                                         "sensor group"],
      ["bundle written and valid", "labelFree=true, labelsUsed=0, safetyMode=ADVISORY, layers carry the "
                                    "fitted weights"]],
     [2.5, 3.8]),

    ("h1", "Label-free separation results", "6"),
    ("p", "The healthy baseline (fitted on healthy data only): mean 0.78, p99 2.08, p999 2.75 in "
          "reconstruction-error units. Against that baseline, every injected degradation — bearing wear, "
          "cavitation, sensor drift, energy deterioration, and control oscillation — rises above **twice** "
          "the p999 extreme at full ramp, and the dominant residual identifies the correct sensor group in "
          "each case. Healthy assets exceed their own p99 less than 5% of the time, and the persistence "
          "requirement in the evidence accumulator suppresses most of those transients before they become "
          "advisories."),
    ("callout", "Why this matters",
     "It shows the label-free core genuinely separates faults from normal using nothing but the coupling "
     "between sensors — no failure history was consulted at any point.", "info"),

    ("h1", "Two bugs the tests caught", "7"),
    ("p", "The smoke and unit tests surfaced two genuine latent defects, both fixed and now regression-"
          "guarded:"),
    ("bullet", "**Advisory-gate dedup overflow.** The per-asset de-duplication used a sentinel of "
               "Long.MIN_VALUE; `timestamp − sentinel` overflows for real epoch-millisecond timestamps, "
               "which made the check wrongly suppress the **first** advisory for every asset."),
    ("bullet", "**Feedback rate-limit overflow.** The same sentinel pattern in the adapter would have "
               "swallowed the **first** feedback update per family in production."),
    ("p", "Both are now a null check instead of a sentinel subtraction, with dedicated tests "
          "(`first advisory not suppressed`, `first event is not suppressed`) that use realistic "
          "epoch-style timestamps."),

    ("h1", "Determinism and reproducibility", "8"),
    ("p", "Training and the synthetic corpus are seeded, so results are bit-for-bit reproducible. The fault "
          "timings live only in the scoring oracle; they never enter the fit. Both suites run offline with "
          "no third-party Python dependencies."),

    ("h1", "What these results prove — and what they don't", "9"),
    ("bullet", "**They prove** the runtime logic is correct, the safety invariant holds, and the label-free "
               "fit separates the modelled fault families on realistic synthetic data."),
    ("bullet", "**They do not prove** field accuracy on your plant. Synthetic-corpus separation is not a "
               "field detection rate; real assets are messier, and fault families are heuristic until "
               "confirmed events accumulate."),
    ("bullet", "**Therefore:** deploy in shadow first, measure the false-positive rate on your data, and "
               "let the feedback loop calibrate — exactly the rollout the Deployment Guide prescribes."),

    ("h1", "How to reproduce", "10"),
    ("code", "# python training suite\n"
             "cd scripts/demo-self-supervised-maintenance\n"
             "python -m unittest tests.test_ss_maintenance -v\n\n"
             "# java runtime suite\n"
             "mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test"),
    ("pi", "The tests hold the model to an honest bar: detect faults with no labels, never actuate, and "
           "improve from feedback — and they earned their keep by catching two real bugs before "
           "production."),
]


_META_UK = [
    ("Документ", "Звіт про тестування"),
    ("Продукт", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Java-набір", "SelfSupervisedMaintenanceModuleTest — 14/14 успішно"),
    ("Python-набір", "tests/test_ss_maintenance.py — 5/5 успішно"),
    ("Метод", "Детермінований; мітки відмов лише для оцінки, ніколи для навчання"),
    ("Автор", "Дмитро Раковський — Харків, Україна"),
    ("Дата", DATE_UK),
    ("Ліцензія", "BSD 3-Clause"),
]

_UK = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Звіт про тестування",
     "Що тестували, що пройшло і що означають результати — включно з двома реальними помилками, які "
     "виявили тести",
     _META_UK, "Звіт про тестування",
     "Зрозумілий виклад перевірки: Java-набір середовища виконання, Python-набір навчання, результати "
     "відокремлення без міток, дві приховані помилки, які виявили тести, та їх виправлення, і чесна заява "
     "про те, що ці результати доводять, а що ні."),

    ("toc", "Зміст",
     ["Підсумок",
      "Для нефахівця: як ми тестували без міток",
      "Що було під тестом",
      "Java-набір середовища виконання (14 тестів)",
      "Python-набір навчання (5 тестів)",
      "Результати відокремлення без міток",
      "Дві помилки, які виявили тести",
      "Детермінізм і відтворюваність",
      "Що ці результати доводять — а що ні",
      "Як відтворити"]),

    ("h1", "Підсумок", "1"),
    ("table", ["Набір", "Тести", "Результат"],
     [["Java-середовище (SelfSupervisedMaintenanceModuleTest)", "14", "усі успішно"],
      ["Python-навчання (test_ss_maintenance)", "5", "усі успішно"],
      ["Знайдені й виправлені приховані помилки", "2", "переповнення в дедуп і обмеженні частоти"]],
     [3.4, 1.2, 1.7]),
    ("callout", "Головне",
     "Кожне впроваджене сімейство несправностей виявляється без жодної мітки, здорові активи мовчать, цикл "
     "зворотного зв'язку рухає пороги у правильний бік, і модель доказово не може керувати. Дві реальні "
     "помилки переповнення міток часу знайдено тестами й виправлено.", "success"),

    ("h1", "Для нефахівця: як ми тестували без міток", "2"),
    ("p", "Керовану модель тестувати легко: ховаєш кілька розмічених відмов і перевіряєш, чи вона їх "
          "знаходить. У цієї моделі міток немає, тож тест інший. Ми генеруємо реалістичну телеметрію, "
          "потайки впроваджуємо повільну деградацію в деякі активи й тримаємо час цієї деградації в "
          "запечатаному «ключі відповідей», якого модель не бачить. Модель навчається лише на здорових "
          "даних. Потім перевіряємо: чи позначила вона активи, що деградують (використовуючи ключ лише для "
          "оцінки), і чи лишила здорові в спокої? Це чесний тест детектора без міток."),

    ("h1", "Що було під тестом", "3"),
    ("bullet", "**Середовище (Java):** чотири нейрони, шість сигналів і п'ять процесорів — модель, що "
               "справді працює у продакшені."),
    ("bullet", "**Навчання (Python):** припасування без міток і розгортувана збірка, яку воно генерує."),
    ("bullet", "**Інваріанти безпеки:** що модель рекомендаційна й не може бути перемкнена на керування."),

    ("h1", "Java-набір середовища виконання (14 тестів)", "4"),
    ("p", "Виконано через справжню платформу JUnit. Покриття за областями:"),
    ("table", ["Область", "Що стверджується"],
     [["Відновлення", "здоровий кадр → малий залишок; аномалія → великий залишок на правильному сенсорі; "
                     "позначено зсув домену"],
      ["Гіпотеза", "зростаючий залишок накопичує свідчення, підіймає тяжкість, приписує сімейство й дає "
                   "запас часу; здоровий потік мовчить"],
      ["Зворотний зв'язок", "хибне спрацювання підіймає поріг, підтвердження послаблює, адаптація "
                            "замерзає під час зсуву, перше оновлення не придушується"],
      ["Рекомендаційний шлюз", "спрацьовує за порогом, дедуплікує, застосовує живі оновлення порога, "
                              "перша рекомендація не придушується, advisoryOnly true"],
      ["Конфіг / процесори", "інваріант «лише рекомендація» тримається; усі процесори типізовані через "
                             "інтерфейс"]],
     [1.7, 4.6]),

    ("h1", "Python-набір навчання (5 тестів)", "5"),
    ("table", ["Тест", "Що підтверджує"],
     [["перцентилі впорядковані", "mean < p95 < p99 < p999"],
      ["ваги скінченні", "кожна припасована вага — скінченне число"],
      ["здорові кадри — малий залишок", "здоровий актив перевищує власний p99 менш ніж у 5% випадків"],
      ["несправності відокремлюються без міток", "кожне сімейство понад 2× p999 і вказує на правильну "
                                                 "групу сенсорів"],
      ["збірка записана й валідна", "labelFree=true, labelsUsed=0, safetyMode=ADVISORY, шари несуть "
                                    "припасовані ваги"]],
     [2.5, 3.8]),

    ("h1", "Результати відокремлення без міток", "6"),
    ("p", "Здоровий базовий рівень (припасований лише на здорових даних): mean 0.78, p99 2.08, p999 2.75 в "
          "одиницях помилки відновлення. Відносно цього базового рівня кожна впроваджена деградація — знос "
          "підшипника, кавітація, дрейф сенсора, погіршення енергоефективності та коливання контуру — "
          "піднімається понад **вдвічі** вище екстремуму p999 на повному розгоні, і домінантний залишок "
          "визначає правильну групу сенсорів у кожному випадку. Здорові активи перевищують власний p99 менш "
          "ніж у 5% випадків, а вимога стійкості в накопичувачі свідчень придушує більшість цих перехідних "
          "процесів перш ніж вони стануть рекомендаціями."),
    ("callout", "Чому це важливо",
     "Це показує, що ядро без міток справді відокремлює несправності від норми, спираючись лише на зв'язок "
     "між сенсорами — історія відмов не залучалася жодного разу.", "info"),

    ("h1", "Дві помилки, які виявили тести", "7"),
    ("p", "Димові й модульні тести виявили два справжні приховані дефекти, обидва виправлено й тепер "
          "захищено від регресії:"),
    ("bullet", "**Переповнення дедупу шлюзу.** Дедуплікація за активом використовувала сентинел "
               "Long.MIN_VALUE; `timestamp − sentinel` переповнюється на реальних мітках часу в "
               "мілісекундах, що хибно придушувало **першу** рекомендацію для кожного активу."),
    ("bullet", "**Переповнення обмеження частоти зворотного зв'язку.** Той самий шаблон сентинела в "
               "адаптері проковтнув би **перше** оновлення зворотного зв'язку для кожного сімейства у "
               "продакшені."),
    ("p", "Обидва тепер є перевіркою на null замість віднімання сентинела, з окремими тестами "
          "(«перша рекомендація не придушується», «перша подія не придушується»), що використовують "
          "реалістичні мітки часу епохи."),

    ("h1", "Детермінізм і відтворюваність", "8"),
    ("p", "Навчання й синтетичний корпус мають фіксоване зерно, тож результати відтворюються біт у біт. "
          "Час несправностей живе лише в оракулі оцінювання; він ніколи не входить у припасування. Обидва "
          "набори працюють офлайн без сторонніх Python-залежностей."),

    ("h1", "Що ці результати доводять — а що ні", "9"),
    ("bullet", "**Вони доводять**, що логіка середовища виконання коректна, інваріант безпеки тримається, а "
               "припасування без міток відокремлює змодельовані сімейства несправностей на реалістичних "
               "синтетичних даних."),
    ("bullet", "**Вони не доводять** польову точність на вашій установці. Відокремлення на синтетичному "
               "корпусі — це не польова частка виявлення; реальні активи безладніші, а сімейства "
               "несправностей евристичні, доки не накопичаться підтверджені події."),
    ("bullet", "**Тому:** спершу розгортайте в тіні, вимірюйте частку хибних спрацювань на своїх даних і "
               "дайте циклу зворотного зв'язку відкалібрувати — саме така розкатка, що приписує посібник з "
               "розгортання."),

    ("h1", "Як відтворити", "10"),
    ("code", "# python-набір навчання\n"
             "cd scripts/demo-self-supervised-maintenance\n"
             "python -m unittest tests.test_ss_maintenance -v\n\n"
             "# java-набір середовища виконання\n"
             "mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test"),
    ("pi", "Тести тримають модель на чесній планці: виявляти несправності без міток, ніколи не керувати й "
           "вдосконалюватися зі зворотного зв'язку — і вони виправдали себе, зловивши дві реальні помилки "
           "до продакшену."),
]
