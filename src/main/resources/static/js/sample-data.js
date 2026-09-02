// Same scenarios as src/main/resources/sample-data/ (kept in sync manually).
// Powers the "try a demo" chips so the form can be pre-filled without a round trip.
const DEMO_SCENARIOS = {
  bridge: {
    referenceOutlet: "Continental Wire",
    referenceText: `RIVERBEND — The Route 9 overpass on the eastern span of the Alder River Bridge collapsed at approximately 6:14 a.m. local time on Tuesday, sending two vehicles into the river below, regional emergency officials said.

Fire and rescue crews confirmed 12 people were killed in the collapse. Authorities cautioned that the death toll could rise as search and recovery operations continue through the week, and that the number should be treated as provisional.

"We are still in an active search phase, and I don't want anyone to treat this number as final," said Regional Fire Marshal Elena Ruiz at a Tuesday afternoon briefing.

The Department of Transportation estimated the cost of emergency repairs and lost infrastructure at $4.2 million, based on a preliminary engineering assessment.

A 2019 structural inspection had flagged corrosion in the bridge's support trusses. Budget documents show that a full truss replacement, estimated at $1.8 million, had been delayed twice due to funding shortfalls, and inspectors had recommended weight restrictions that were never implemented. Transportation officials said the delayed maintenance is one of several factors under review, and cautioned that a definitive cause would not be known until the National Transportation Safety Board completes its investigation, which typically takes 12 to 18 months.

Governor Adeyemi ordered inspections of 40 similar-era bridges statewide as a precaution.`,
    comparisons: [
      {
        outlet: "Metro Herald",
        text: `BRIDGE HORROR: 18 DEAD as neglected overpass collapses into river

The Route 9 overpass on the Alder River Bridge collapsed Tuesday morning, plunging vehicles into the water below. Officials say 18 people have died in the disaster.

The Department of Transportation says emergency repairs will cost the state $4.2 million.

A 2019 inspection had flagged corrosion in the bridge's support trusses, and officials had delayed a planned truss replacement twice due to budget shortfalls. Years of underfunding caused the bridge to collapse, transportation watchdogs say, and inspectors' warnings about weight limits were repeatedly ignored.

"This bridge was a disaster waiting to happen," Fire Marshal Elena Ruiz said, describing the scene as chaos.

Governor Adeyemi has ordered emergency inspections statewide.`
      },
      {
        outlet: "Daily Ledger",
        text: `The eastern span of the Alder River Bridge on Route 9 gave way early Tuesday morning, around 6:14 a.m., dropping two vehicles into the Alder River, according to regional emergency officials.

Twelve people were killed, fire and rescue crews confirmed, though officials warned that number is preliminary and could rise as search and recovery work continues through the week.

"We are still in an active search phase, and I don't want anyone to treat this number as final," Regional Fire Marshal Elena Ruiz said Tuesday afternoon.

The state Department of Transportation put the cost of emergency repairs and lost infrastructure at roughly $4.2 million, citing a preliminary engineering assessment.

A structural inspection in 2019 had already flagged corrosion in the bridge's support trusses, and budget records show a planned $1.8 million truss replacement had been delayed twice over funding shortfalls. Inspectors had also recommended weight restrictions that were never put in place. Transportation officials said the delayed maintenance is one of several factors under review, and said a definitive cause will not be known until the National Transportation Safety Board finishes its investigation, a process that usually takes 12 to 18 months.

Governor Adeyemi has ordered inspections of 40 similar-era bridges statewide as a precaution.`
      }
    ]
  },

  drink: {
    referenceOutlet: "Lindgren Institute Release",
    referenceText: `The Lindgren Institute for Public Health today released findings from a 6-month observational study of 1,200 adults who reported daily consumption of high-caffeine energy beverages.

Participants who drank the beverage daily reported headaches at higher rates than the control group, with 35% reporting at least one headache per week, compared to 21% in the control group.

"Our data show an association between daily consumption and increased headache frequency, but this was an observational study, not a controlled trial, so we cannot say the beverage causes headaches," said Dr. Priya Nakamura, the study's lead author. "Dehydration, sleep disruption, and caffeine withdrawal are all plausible contributing factors that our design cannot rule out."

The study found no statistically significant association between beverage consumption and blood pressure changes over the study period.

Dr. Nakamura said the findings warrant a larger, controlled follow-up study before any clinical recommendations are made.`,
    comparisons: [
      {
        outlet: "Viral Health Blog",
        text: `New Study Proves Your Energy Drink Habit Is Giving You Headaches

A new study from the Lindgren Institute found that energy drinks cause headaches, with 35% of daily drinkers affected compared to just 21% of non-drinkers.

"Our data show an association between daily consumption and increased headache frequency," said Dr. Priya Nakamura, the study's lead author.

Researchers tracked 1,200 adults over six months. Experts say the results should make people think twice before reaching for another can.`
      }
    ]
  },

  fire: {
    referenceOutlet: "County OEM Briefing",
    referenceText: `The Pinecrest Ridge Fire had burned an estimated 8,400 acres as of Wednesday evening and was 15% contained, according to the County Office of Emergency Management.

Approximately 3,200 residents remain under mandatory evacuation orders across four zones. Officials estimated that between 150 and 200 structures have been damaged or destroyed, but cautioned that the figure is a preliminary aerial estimate and will be revised once ground assessment teams can safely access the area.

"We understand how difficult it is to be displaced from your home, and we are working around the clock, but I need to be clear that our damage numbers today are estimates, not a final count," said OEM Director Marcus Chen at Wednesday's press briefing.

No fatalities have been confirmed. Two firefighters sustained non-life-threatening injuries.

Officials said containment is expected to improve significantly once weekend winds are forecast to weaken, but stressed that the fire's behavior remains unpredictable.`,
    comparisons: [
      {
        outlet: "Northlight Times",
        text: `The Pinecrest Ridge Fire has burned an estimated 8,400 acres and was 15% contained as of Wednesday evening, the County Office of Emergency Management said.

About 3,200 residents are under mandatory evacuation orders. Officials said 200 structures have been destroyed in the blaze.

"We are working around the clock for this community," said OEM Director Marcus Chen.

No fatalities have been confirmed, and two firefighters suffered minor injuries.`
      }
    ]
  }
};
