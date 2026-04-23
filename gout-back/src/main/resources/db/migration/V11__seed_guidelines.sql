-- 통풍 관리 가이드라인 시드 데이터 (DO 10 + DONT 10 이상)
-- 주요 근거: 2020 ACR Guidelines for Management of Gout (DOI 10.1002/acr.24180)
--           EULAR 2016 recommendations (DOI 10.1136/annrheumdis-2016-209707)
--           Choi HK et al., NEJM 2004 / Lancet 2004 / BMJ 2008
--           Zhang Y et al., Arthritis Rheum 2012 (체리)
--           Neogi T et al., Ann Rheum Dis 2014 (알코올)

INSERT INTO guidelines (type, category, title, content, evidence_strength, evidence_source, evidence_doi, target_age_groups, is_published) VALUES

-- ============ DO (권장 사항) 10개 ============
('DO', 'LIFESTYLE', '하루 물 2리터 이상 마시기',
 '충분한 수분 섭취는 신장을 통한 요산 배설을 촉진합니다. 특히 요산석 예방과 발작 빈도 감소에 효과적입니다. 커피, 차 등도 수분으로 계산 가능하나 단순 물이 가장 안전합니다.',
 'STRONG', '2020 ACR Guidelines for Gout', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DO', 'FOOD', '저지방 유제품 매일 섭취',
 '저지방 우유, 요거트는 통풍 발병률을 낮추고 요산 수치를 감소시킵니다. 하루 1-2잔을 권장합니다.',
 'STRONG', 'Choi HK et al., NEJM 2004', '10.1056/NEJMoa035700', ARRAY['all'], TRUE),

('DO', 'FOOD', '체리/블랙베리 등 항산화 과일 섭취',
 '체리는 통풍 발작 위험을 약 35% 감소시키는 것으로 보고되었습니다. 하루 10-12개(또는 체리 주스 1컵) 권장.',
 'MODERATE', 'Zhang Y et al., Arthritis Rheum 2012', '10.1002/art.34677', ARRAY['all'], TRUE),

('DO', 'FOOD', '커피 하루 2-4잔',
 '커피는 요산 감소 및 통풍 위험 감소와 연관이 있습니다. 카페인 유무와 무관하게 유익하나 설탕/시럽은 피해야 합니다.',
 'MODERATE', 'Choi HK et al., Arthritis Rheum 2007', '10.1002/art.22762', ARRAY['30s','40s','50s','60s','70s+'], TRUE),

('DO', 'EXERCISE', '저강도 유산소 운동 주 150분',
 '걷기, 수영, 자전거 등 관절 부담이 낮은 유산소 운동을 권장합니다. 체중 감량은 요산 수치 개선에 직접적 효과가 있습니다.',
 'STRONG', '2020 ACR Guidelines / EULAR 2016', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DO', 'LIFESTYLE', '점진적 체중 감량(BMI 25 이하)',
 '비만은 요산 증가와 통풍 발병의 주요 위험 인자입니다. 급격한 단식은 오히려 요산을 상승시키므로 서서히 감량합니다.',
 'STRONG', 'EULAR 2016 recommendations', '10.1136/annrheumdis-2016-209707', ARRAY['30s','40s','50s','60s'], TRUE),

('DO', 'MEDICATION', '처방받은 요산 저하제(알로푸리놀/페북소스타트) 꾸준히 복용',
 '증상이 없을 때도 요산 저하제는 지속 복용해야 합니다. 목표 요산치는 6 mg/dL 미만(토푸스 있으면 5 미만)입니다.',
 'STRONG', '2020 ACR Guidelines for Gout', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DO', 'MEDICATION', '요산 저하제 시작 시 발작 예방제 동시 복용',
 '알로푸리놀 시작 초기 3-6개월은 콜히친 또는 저용량 NSAID를 예방적으로 병용해 reflex flare를 방지합니다.',
 'STRONG', '2020 ACR Guidelines', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DO', 'EMERGENCY', '급성 발작 시 조기(24시간 내) 치료 시작',
 'NSAID, 콜히친, 스테로이드 중 하나를 가능한 한 빨리 시작하면 증상 기간이 크게 단축됩니다. 즉시 냉찜질도 도움이 됩니다.',
 'STRONG', '2020 ACR Guidelines / EULAR 2016', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DO', 'FOOD', '채소와 식물성 단백질 위주 식단(DASH/지중해식)',
 'DASH식과 지중해식 식단은 요산을 낮추고 통풍 발작을 줄입니다. 채소 퓨린은 통풍과 관련성이 없습니다.',
 'MODERATE', 'Rai SK et al., BMJ 2017', '10.1136/bmj.j1794', ARRAY['all'], TRUE),

-- ============ DONT (금기 사항) 10개 ============
('DONT', 'FOOD', '알코올 음료, 특히 맥주 피하기',
 '알코올은 요산 생성을 증가시키고 신장 배설을 억제합니다. 맥주는 퓨린도 포함해 이중으로 해롭습니다. 발작 시에는 완전 금주, 완화기에도 최소화합니다.',
 'STRONG', 'Choi HK et al., Lancet 2004 / Neogi T et al., Ann Rheum Dis 2014',
 '10.1016/S0140-6736(04)15995-0', ARRAY['all'], TRUE),

('DONT', 'FOOD', '내장류(간, 신장, 곱창) 섭취 금지',
 '내장류는 퓨린 함량이 가장 높은 식품군으로 소량으로도 발작을 유발할 수 있습니다.',
 'STRONG', '2020 ACR Guidelines', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DONT', 'FOOD', '과당 첨가 음료(탄산음료, 가당 주스) 금지',
 '과당은 간에서 요산 생성을 급격히 증가시킵니다. 100% 과일 주스도 과량은 주의하며 탄산음료는 완전 회피합니다.',
 'STRONG', 'Choi HK et al., BMJ 2008', '10.1136/bmj.39449.819271.BE', ARRAY['all'], TRUE),

('DONT', 'FOOD', '멸치/정어리/어란 등 고퓨린 해산물 제한',
 '멸치, 정어리, 명란, 가쓰오부시 등은 퓨린이 매우 높아 AVOID 대상입니다. 국물용으로도 주의합니다.',
 'STRONG', '2020 ACR Guidelines', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DONT', 'LIFESTYLE', '급격한 단식/원푸드 다이어트 금지',
 '단식 상태에서 케톤이 축적되면 요산 배설이 억제되어 오히려 발작을 유발합니다. 감량은 서서히 진행합니다.',
 'MODERATE', 'EULAR 2016 recommendations', '10.1136/annrheumdis-2016-209707', ARRAY['30s','40s','50s'], TRUE),

('DONT', 'MEDICATION', '발작 중 요산 저하제 용량을 임의 조정하지 말기',
 '이미 복용 중인 알로푸리놀 등은 발작 중에도 중단하지 않고 동일 용량을 유지합니다. 새로 시작하거나 증량은 발작 가라앉은 뒤에 합니다.',
 'STRONG', '2020 ACR Guidelines', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DONT', 'MEDICATION', '아스피린 저용량의 장기 사용 자가 판단 금지',
 '저용량 아스피린(75-325mg)은 요산 배설을 억제해 통풍을 악화시킬 수 있습니다. 심혈관 목적 복용은 주치의와 상의 필요.',
 'MODERATE', 'Caspi D et al., Arthritis Rheum 2000', '10.1002/1529-0131(200001)43:1', ARRAY['50s','60s','70s+'], TRUE),

('DONT', 'EXERCISE', '발작 중 강한 운동/마사지 금지',
 '급성 발작 중 환부에 압력/열을 가하면 염증이 악화됩니다. 안정과 거상(elevation)이 우선입니다.',
 'STRONG', '2020 ACR Guidelines', '10.1002/acr.24180', ARRAY['all'], TRUE),

('DONT', 'FOOD', '설탕/시럽 추가 음료 금지',
 '과당/설탕 첨가는 요산 합성을 증가시킵니다. 스무디, 가당 커피, 에너지 드링크 모두 주의합니다.',
 'STRONG', 'Choi HK, BMJ 2008', '10.1136/bmj.39449.819271.BE', ARRAY['all'], TRUE),

('DONT', 'EMERGENCY', '발작 시 자가 진통제 과다 복용 금지',
 'NSAID는 위장출혈/신장 부담을, 콜히친은 과용시 중독을 일으킵니다. 용법을 지키고 증상 지속 시 진료를 받습니다.',
 'STRONG', '2020 ACR Guidelines / 식약처 안전사용 가이드',
 '10.1002/acr.24180', ARRAY['all'], TRUE),

-- ============ 연령대별 추가 DO/DONT ============
('DO', 'LIFESTYLE', '20-30대: 음주 습관 조기 교정',
 '젊은 통풍 환자 증가의 주 원인은 잦은 폭음과 가당 음료입니다. 20-30대부터 음주 빈도를 낮추면 장기 예후가 크게 개선됩니다.',
 'MODERATE', 'Kuo CF et al., Nat Rev Rheumatol 2015', '10.1038/nrrheum.2015.91', ARRAY['20s','30s'], TRUE),

('DONT', 'FOOD', '50대+: 나트륨 과다 섭취 주의',
 '고혈압 동반 비율이 높은 연령대로, 짠 국물/젓갈류는 혈압과 신장 기능 모두에 악영향을 주어 통풍 관리도 어렵게 합니다.',
 'MODERATE', 'EULAR 2016 / 한국고혈압학회 2022', '10.1136/annrheumdis-2016-209707', ARRAY['50s','60s','70s+'], TRUE);
