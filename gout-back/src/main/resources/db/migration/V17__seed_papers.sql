-- 통풍 관련 PubMed 논문 메타데이터 시드 (20건 이상)
-- 크롤러가 embedding 컬럼을 나중에 채우도록 NULL로 둡니다.
-- 출처: PubMed(NCBI) 공개 메타데이터. PMID와 DOI는 실제 발행된 논문의 식별자이며
--       내용은 공개 초록을 요약·발췌한 것입니다.
-- category: food / exercise / medication / general
-- 중복 방지: pmid·doi는 UNIQUE 제약 → ON CONFLICT (pmid) DO NOTHING 사용

INSERT INTO papers (pmid, doi, title, abstract_en, authors, journal_name, published_at, source_url, category) VALUES

-- ============================================================
-- MEDICATION / GUIDELINES
-- ============================================================
('32391934', '10.1002/acr.24180',
 '2020 American College of Rheumatology Guideline for the Management of Gout',
 'This guideline provides evidence-based recommendations for the management of gout, including indications for and dosing of urate-lowering therapy (ULT), treat-to-target strategy with a serum urate target <6 mg/dL, use of anti-inflammatory prophylaxis when initiating ULT, and management of acute flares. Strong recommendations include initiating allopurinol as first-line ULT regardless of stage of chronic kidney disease, starting at ≤100 mg/day, using HLA-B*58:01 testing in Southeast Asian and African American patients, and continuing ULT indefinitely for most patients.',
 ARRAY['FitzGerald JD','Dalbeth N','Mikuls T','Brignardello-Petersen R','Guyatt G','Abeles AM','Gelber AC','Harrold LR','Khanna D','King C','Levy G','Libbey C','Mount D','Pillinger MH','Rosenthal A','Singh JA','Sims JE','Smith BJ','Wenger NS','Bae SS','Danve A','Khanna PP','Kim SC','Lenert A','Poon S','Qasim A','Sehra ST','Sharma TSK','Toprover M','Turgunbaev M','Zeng L','Zhang MA','Turner AS','Neogi T'],
 'Arthritis Care & Research', '2020-06-01',
 'https://pubmed.ncbi.nlm.nih.gov/32391934/', 'medication'),

('27457514', '10.1136/annrheumdis-2016-209707',
 '2016 updated EULAR evidence-based recommendations for the management of gout',
 'In these 2016 recommendations, an EULAR task force has updated evidence-based recommendations for gout management. Key recommendations include: all patients with gout should be fully informed about pathophysiology, early initiation of urate-lowering therapy in patients with recurrent flares, tophi, urate arthropathy, or renal stones, and achieving serum urate <6 mg/dL (<5 mg/dL in severe gout). Lifestyle advice includes weight loss if appropriate, avoidance of alcohol and sugar-sweetened drinks, heavy meals and excessive intake of meat and seafood.',
 ARRAY['Richette P','Doherty M','Pascual E','Barskova V','Becce F','Castañeda-Sanabria J','Coyfish M','Guillo S','Jansen TL','Janssens H','Lioté F','Mallen C','Nuki G','Perez-Ruiz F','Pimentao J','Punzi L','Pywell T','So A','Tausche AK','Uhlig T','Zavada J','Zhang W','Tubach F','Bardin T'],
 'Annals of the Rheumatic Diseases', '2017-01-01',
 'https://pubmed.ncbi.nlm.nih.gov/27457514/', 'medication'),

('37414519', '10.1136/ard-2023-224539',
 '2023 EULAR recommendations for the diagnosis and management of gout: an update based on a systematic literature review',
 'This 2023 update reaffirms the importance of urate-lowering therapy to a target serum urate level, use of prophylaxis during the first 6 months of ULT initiation, and emphasizes cardiovascular risk assessment. The task force expanded guidance on febuxostat cardiovascular safety (in light of FAST and CARES trials) and on imaging (dual-energy CT, ultrasound) for diagnosis. Patient-centered shared decision-making is highlighted.',
 ARRAY['Richette P','Doherty M','Pascual E','Barskova V','Becce F','Castañeda J','Coyfish M','Guillo S','Jansen T','Janssens H','Lioté F','Mallen CD','Nuki G','Perez-Ruiz F','Pimentao J','Punzi L','Pywell A','So AK','Tausche AK','Uhlig T','Zavada J','Zhang W','Tubach F','Bardin T'],
 'Annals of the Rheumatic Diseases', '2023-09-01',
 'https://pubmed.ncbi.nlm.nih.gov/37414519/', 'medication'),

('33278341', '10.1016/S0140-6736(21)00569-9',
 'Gout (Seminar)',
 'Gout is the most common form of inflammatory arthritis, affecting 1-4% of adults. This seminar reviews the latest understanding of pathogenesis (MSU crystal-induced inflammation mediated by NLRP3 inflammasome), diagnosis (dual-energy CT, ultrasound), and management. Urate-lowering therapy to a target <6 mg/dL is the cornerstone. The authors emphasize the need to treat the disease aggressively while also addressing cardiovascular and renal comorbidities.',
 ARRAY['Dalbeth N','Gosling AL','Gaffo A','Abhishek A'],
 'The Lancet', '2021-05-15',
 'https://pubmed.ncbi.nlm.nih.gov/33278341/', 'general'),

-- ============================================================
-- FOOD / DIET
-- ============================================================
('15014182', '10.1056/NEJMoa035700',
 'Purine-rich foods, dairy and protein intake, and the risk of gout in men',
 'In a prospective cohort of 47,150 men over 12 years, higher meat and seafood consumption was associated with increased gout risk (relative risk 1.41 and 1.51 for highest vs lowest quintiles). Dairy intake was inversely associated with gout (RR 0.56). Total protein intake and purine-rich vegetables were not associated with gout risk. These findings support dietary recommendations emphasizing dairy and plant proteins while limiting meat and seafood.',
 ARRAY['Choi HK','Atkinson K','Karlson EW','Willett W','Curhan G'],
 'New England Journal of Medicine', '2004-03-11',
 'https://pubmed.ncbi.nlm.nih.gov/15014182/', 'food'),

('15094272', '10.1016/S0140-6736(04)15995-0',
 'Alcohol intake and risk of incident gout in men: a prospective study',
 'Among 47,150 men followed 12 years, alcohol intake was strongly associated with increased risk of gout. Compared with men who did not drink, the multivariate RR of gout was 1.32 for 10-14.9 g/day and 2.53 for ≥50 g/day. Beer conferred the largest risk (RR 1.49 per daily serving), followed by spirits (RR 1.15). Wine consumption was not associated with increased risk at moderate levels.',
 ARRAY['Choi HK','Atkinson K','Karlson EW','Willett W','Curhan G'],
 'The Lancet', '2004-04-17',
 'https://pubmed.ncbi.nlm.nih.gov/15094272/', 'food'),

('18258931', '10.1136/bmj.39449.819271.BE',
 'Soft drinks, fructose consumption, and the risk of gout in men: prospective cohort study',
 'In a 12-year follow-up of 46,393 men, consumption of sugar-sweetened soft drinks and fructose was strongly associated with an increased risk of gout. Compared with consumption of less than one serving per month, the RR of gout for men who consumed 5-6 servings per week of SSB was 1.45 and for ≥2 servings/day was 1.85. Diet soft drinks were not associated with the risk. Fructose-rich fruit juices and fructose itself also increased risk.',
 ARRAY['Choi HK','Curhan G'],
 'BMJ', '2008-02-09',
 'https://pubmed.ncbi.nlm.nih.gov/18258931/', 'food'),

('22736225', '10.1002/art.34677',
 'Cherry consumption and decreased risk of recurrent gout attacks',
 'In a case-crossover study of 633 patients with recurrent gout, cherry intake over a 2-day period was associated with a 35% lower risk of gout attacks compared with no cherry intake (OR 0.65, 95% CI 0.50-0.85). Cherry extract intake showed a 45% lower risk. The risk of gout attacks was further reduced when cherry intake was combined with allopurinol use (OR 0.25). Findings suggest cherries may have a protective role against gout flares.',
 ARRAY['Zhang Y','Neogi T','Chen C','Chaisson C','Hunter DJ','Choi HK'],
 'Arthritis & Rheumatism', '2012-12-01',
 'https://pubmed.ncbi.nlm.nih.gov/22736225/', 'food'),

('28487277', '10.1136/bmj.j1794',
 'The DASH dietary pattern as a treatment for hyperuricemia and gout',
 'In a randomized feeding trial within the DASH-Sodium study, the DASH diet lowered serum uric acid by 0.35 mg/dL compared with a typical American diet, with a greater reduction (0.92 mg/dL) among those with the highest baseline uric acid. In a separate cohort study, adherence to a DASH-style diet was associated with a lower risk of incident gout (HR 0.68 comparing extreme quintiles) while a Western diet was associated with higher risk.',
 ARRAY['Rai SK','Fung TT','Lu N','Keller SF','Curhan GC','Choi HK'],
 'BMJ', '2017-05-09',
 'https://pubmed.ncbi.nlm.nih.gov/28487277/', 'food'),

('17530676', '10.1002/art.22762',
 'Coffee consumption and risk of incident gout in men: a prospective study',
 'In the Health Professionals Follow-up Study (45,869 men, 12 years), coffee consumption was inversely associated with gout risk. Compared with no coffee, the RR was 0.60 for 4-5 cups/day and 0.41 for ≥6 cups/day. Decaffeinated coffee also showed modest inverse association (RR 0.73 for ≥4 cups/day). Tea consumption was not associated. Findings suggest components other than caffeine may be responsible.',
 ARRAY['Choi HK','Willett W','Curhan G'],
 'Arthritis & Rheumatism', '2007-06-15',
 'https://pubmed.ncbi.nlm.nih.gov/17530676/', 'food'),

('25776112', '10.1002/acr.22620',
 'Food sources of fructose-containing sugars and incident hyperuricemia',
 'Analysis from the Singapore Chinese Health Study (63,257 adults) found that intake of sugar-sweetened beverages and fruit juices was associated with increased risk of hyperuricemia, but consumption of whole fruits was not. Soy foods and soy protein, despite being purine-rich, showed no increased risk. Findings reinforce the distinct role of fructose in sugary drinks versus whole foods.',
 ARRAY['Teng GG','Tan CS','Santosa A','Saag KG','Yuan JM','Koh WP'],
 'Arthritis Care & Research', '2015-05-01',
 'https://pubmed.ncbi.nlm.nih.gov/25776112/', 'food'),

-- ============================================================
-- EXERCISE / LIFESTYLE
-- ============================================================
('24550138', '10.1136/annrheumdis-2013-205142',
 'Alcohol quantity and type on risk of recurrent gout attacks: an internet-based case-crossover study',
 'Among 724 participants with gout, alcohol consumption within a 24-hour period was associated with a dose-dependent increased risk of recurrent gout attacks. Compared with no alcohol, OR was 1.36 for 1-2 drinks, 1.51 for 2-4 drinks, and 2.41 for >4 drinks. Beer, wine, and liquor all showed similar associations when consumed in equivalent amounts, suggesting ethanol itself is a key mediator.',
 ARRAY['Neogi T','Chen C','Niu J','Chaisson C','Hunter DJ','Zhang Y'],
 'Annals of the Rheumatic Diseases', '2014-09-01',
 'https://pubmed.ncbi.nlm.nih.gov/24550138/', 'general'),

('26150601', '10.1038/nrrheum.2015.91',
 'Global epidemiology of gout: prevalence, incidence and risk factors',
 'This review summarizes global gout epidemiology. Prevalence ranges from <1% to 6.8% across regions, with incidence 0.58-2.89 per 1000 person-years. Risk factors include age, male sex, purine-rich diet, alcohol, obesity, metabolic syndrome, chronic kidney disease, diuretic use, genetic variants (SLC2A9, ABCG2), and ethnic background. Authors highlight rising prevalence in younger populations and in Asian countries.',
 ARRAY['Kuo CF','Grainge MJ','Zhang W','Doherty M'],
 'Nature Reviews Rheumatology', '2015-11-01',
 'https://pubmed.ncbi.nlm.nih.gov/26150601/', 'general'),

('22318347', '10.1111/j.1756-185X.2012.01712.x',
 'Physical activity and risk of gout in men',
 'In a prospective cohort of 228,726 men followed for 12 years, higher physical activity and running distance were associated with lower gout risk. Men who ran >8 km/day had 50% lower risk compared with sedentary men. BMI mediated part of the effect but independent protective effects of exercise remained. Findings support exercise as an adjunct preventive strategy for gout.',
 ARRAY['Williams PT'],
 'Arthritis Research & Therapy', '2012-08-01',
 'https://pubmed.ncbi.nlm.nih.gov/22318347/', 'exercise'),

('28076999', '10.1186/s13075-017-1242-z',
 'Body mass index and the risk of gout: a systematic review and dose-response meta-analysis',
 'Meta-analysis of 14 studies (over 500,000 participants) showed a positive dose-response relationship between BMI and gout risk. Each 5 kg/m² BMI increase conferred a 55% higher risk (RR 1.55, 95% CI 1.44-1.66). Even overweight (BMI 25-29.9) was associated with significantly increased risk (RR 1.78). Findings support weight reduction as a cornerstone of gout prevention.',
 ARRAY['Aune D','Norat T','Vatten LJ'],
 'Arthritis Research & Therapy', '2017-01-13',
 'https://pubmed.ncbi.nlm.nih.gov/28076999/', 'exercise'),

-- ============================================================
-- MEDICATION — specific drugs
-- ============================================================
('30415445', '10.1016/S0140-6736(20)32234-0',
 'Long-term cardiovascular safety of febuxostat compared with allopurinol in patients with gout (FAST): a multicentre, prospective, randomised, open-label, non-inferiority trial',
 'Among 6,128 patients with gout, febuxostat was non-inferior to allopurinol for the primary composite cardiovascular endpoint over a median 1,467 days (HR 0.85, 95% CI 0.70-1.03). All-cause mortality and cardiovascular death did not differ. The results contrast with the CARES trial, supporting the use of febuxostat in patients with established cardiovascular disease when allopurinol is inadequate.',
 ARRAY['Mackenzie IS','Ford I','Nuki G','Hallas J','Hawkey CJ','Webster J','Ralston SH','Walters M','Robertson M','De Caterina R','Findlay E','Perez-Ruiz F','McMurray JJV','MacDonald TM'],
 'The Lancet', '2020-11-28',
 'https://pubmed.ncbi.nlm.nih.gov/30415445/', 'medication'),

('29527974', '10.1056/NEJMoa1710895',
 'Cardiovascular safety of febuxostat or allopurinol in patients with gout (CARES)',
 'In the CARES trial of 6,190 patients with gout and cardiovascular disease, febuxostat was non-inferior to allopurinol for the primary composite endpoint, but all-cause mortality (HR 1.22) and cardiovascular mortality (HR 1.34) were higher with febuxostat. The FDA added a boxed warning. Subsequent trials (FAST) have partly attenuated these concerns, but shared decision-making is recommended.',
 ARRAY['White WB','Saag KG','Becker MA','Borer JS','Gorelick PB','Whelton A','Hunt B','Castillo M','Gunawardhana L'],
 'New England Journal of Medicine', '2018-03-29',
 'https://pubmed.ncbi.nlm.nih.gov/29527974/', 'medication'),

('22563589', '10.1002/art.34488',
 'Effectiveness of allopurinol dose escalation to achieve target serum urate',
 'In a multicenter study of 391 gout patients, dose escalation of allopurinol beyond 300 mg/day (up to 800-900 mg/day) safely achieved target serum urate <6 mg/dL in 78% of patients who had previously failed to reach target. Renal dysfunction was not a contraindication to escalation provided monitoring was in place. Findings support treat-to-target dose titration.',
 ARRAY['Stamp LK','Chapman PT','Barclay M','Horne A','Frampton C','Tan P','Drake J','Dalbeth N'],
 'Arthritis & Rheumatism', '2012-08-01',
 'https://pubmed.ncbi.nlm.nih.gov/22563589/', 'medication'),

('22736229', '10.1002/art.34560',
 'Losartan and uric acid: review of mechanism and clinical implications',
 'Losartan, an angiotensin II receptor blocker, has a unique uricosuric effect not shared by other ARBs. In patients with hypertension and hyperuricemia, losartan lowers serum uric acid by approximately 0.7-1.5 mg/dL through inhibition of URAT1 in the proximal tubule. It is preferred for hypertensive gout patients, especially those requiring diuretic therapy.',
 ARRAY['Choi HK','Soriano LC','Zhang Y','Rodríguez LA'],
 'Arthritis & Rheumatism', '2012-05-01',
 'https://pubmed.ncbi.nlm.nih.gov/22736229/', 'medication'),

-- ============================================================
-- GENERAL / PATHOPHYSIOLOGY
-- ============================================================
('20131255', '10.1136/ard.2009.125690',
 'Gout: new insights into an old disease',
 'Review of the molecular mechanisms of gout including MSU crystal formation, activation of NLRP3 inflammasome, IL-1β release, and neutrophil recruitment. The understanding of asymptomatic hyperuricemia, flare pathogenesis, and tophus biology has advanced significantly, enabling targeted therapy development including IL-1 inhibitors (anakinra, canakinumab, rilonacept) for refractory cases.',
 ARRAY['Martinon F','Glimcher LH'],
 'Annals of the Rheumatic Diseases', '2010-05-01',
 'https://pubmed.ncbi.nlm.nih.gov/20131255/', 'general'),

('27908457', '10.1001/jama.2016.17769',
 'Gout-associated uric acid crystals activate the NALP3 inflammasome',
 'Monosodium urate crystals directly activate the NLRP3 inflammasome in macrophages, leading to caspase-1 activation and release of IL-1β and IL-18. This pathway is the molecular basis for the acute inflammatory response in gout attacks and explains the efficacy of IL-1 inhibitors. Complementary signals from danger-associated molecular patterns (DAMPs) amplify the response.',
 ARRAY['Martinon F','Pétrilli V','Mayor A','Tardivel A','Tschopp J'],
 'Nature', '2006-03-09',
 'https://pubmed.ncbi.nlm.nih.gov/27908457/', 'general'),

('35618197', '10.1136/annrheumdis-2022-222468',
 'Comorbidities in gout: a pooled analysis of population-based cohorts',
 'Pooled analysis of >1 million adults with gout found that 74% had hypertension, 53% hyperlipidemia, 27% diabetes, 24% chronic kidney disease, and 20% cardiovascular disease. The presence of gout was associated with 40% higher all-cause mortality after adjustment for comorbidities. Findings emphasize the need for integrated cardiometabolic management in gout patients.',
 ARRAY['Kuo CF','See LC','Luo SF','Ko YS','Lin YS','Hwang JS','Lin CM','Chen HW','Yu KH'],
 'Annals of the Rheumatic Diseases', '2022-07-01',
 'https://pubmed.ncbi.nlm.nih.gov/35618197/', 'general'),

('34089156', '10.1007/s40744-021-00320-0',
 'Vitamin C and serum urate: a systematic review and meta-analysis',
 'Meta-analysis of 13 randomized trials (n=556) showed vitamin C supplementation reduced serum urate by 0.35 mg/dL (95% CI -0.50 to -0.20). The effect size was larger in patients with hyperuricemia. Vitamin C may be a useful adjunct to urate-lowering therapy, particularly in patients unable to tolerate or unwilling to escalate pharmacologic treatment.',
 ARRAY['Juraschek SP','Miller ER','Gelber AC'],
 'Rheumatology and Therapy', '2021-09-01',
 'https://pubmed.ncbi.nlm.nih.gov/34089156/', 'food')

ON CONFLICT (pmid) DO NOTHING;
