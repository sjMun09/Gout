-- 통풍 환자 음식 마스터 시드 데이터
-- 근거: 2020 ACR Guidelines, Choi HK et al., NEJM 2004 / Lancet 2004, USDA / 일본 식품성분표 퓨린 함량 기준
-- 퓨린 함량 기준: LOW <50 mg/100g, MEDIUM 50-150, HIGH 150-300, VERY_HIGH >300 (mg/100g)

INSERT INTO foods (name, name_en, category, purine_content, purine_level, recommendation, description, caution, evidence_notes) VALUES
-- ============ VERY_HIGH / AVOID (피해야 할 음식) ============
('멸치', 'Anchovy', '해산물', 411.0, 'VERY_HIGH', 'AVOID', '퓨린 함량이 매우 높음. 소량 섭취도 발작 유발 가능', '국물용으로도 피하는 것이 좋음', 'ACR 2020 가이드라인 고퓨린 식품 목록 / USDA DB'),
('정어리', 'Sardine', '해산물', 345.0, 'VERY_HIGH', 'AVOID', '퓨린 함량 최상위 군', '통조림, 구이 모두 주의', 'ACR 2020 / Choi HK, NEJM 2004'),
('소 간', 'Beef Liver', '내장류', 554.0, 'VERY_HIGH', 'AVOID', '내장류 중 퓨린 함량 최고 수준', '소량도 요산 급상승 유발', 'ACR 2020 가이드라인'),
('닭 간', 'Chicken Liver', '내장류', 312.0, 'VERY_HIGH', 'AVOID', '내장류 공통으로 퓨린 매우 높음', '-', 'ACR 2020 가이드라인'),
('돼지 신장', 'Pork Kidney', '내장류', 334.0, 'VERY_HIGH', 'AVOID', '내장류 전반 회피 권고', '-', 'ACR 2020 가이드라인'),
('명란젓', 'Pollock Roe', '해산물', 305.0, 'VERY_HIGH', 'AVOID', '어란류는 퓨린이 매우 높음', '염분도 높아 이중 주의', 'ACR 2020 / 일본 식품성분표'),
('오징어젓', 'Salted Squid', '해산물', 259.0, 'HIGH', 'AVOID', '내장 포함 염장 해산물', '퓨린+염분 이중 부담', 'ACR 2020'),
('가쓰오부시', 'Bonito Flakes', '해산물', 493.0, 'VERY_HIGH', 'AVOID', '농축된 어류 건조품, 퓨린 최상위', '국물용으로도 제한', '일본 식품성분표 퓨린 DB'),
('표고버섯(건조)', 'Dried Shiitake', '채소', 379.0, 'VERY_HIGH', 'AVOID', '건조 과정에서 퓨린 농축됨', '생표고는 중간 수준이나 건조품은 회피', '일본 식품성분표'),
-- ============ HIGH / AVOID-MODERATE (주의해야 할 주류) ============
('맥주', 'Beer', '주류', 14.0, 'HIGH', 'AVOID', '통풍 발작의 가장 흔한 유발 원인', '알코올+퓨린 이중 악영향', 'Choi HK et al., Lancet 2004 363:1277'),
('막걸리', 'Makgeolli', '주류', 12.0, 'MEDIUM', 'AVOID', '발효주 + 효모 잔존으로 퓨린 부담', '한국식 맥주 대체 불가', 'Choi HK, Lancet 2004 / KDA 권고'),
('소주', 'Soju', '주류', 2.0, 'LOW', 'AVOID', '퓨린은 낮으나 알코올 자체가 요산 배설 억제', '절주/금주 권고', 'Choi HK, Lancet 2004'),
('양주(위스키)', 'Whiskey', '주류', 1.5, 'LOW', 'AVOID', '알코올 자체가 요산 배설 억제', '소주와 동일 경고', 'Choi HK, Lancet 2004'),
-- ============ HIGH (제한적 섭취) ============
('고등어', 'Mackerel', '해산물', 194.0, 'HIGH', 'BAD', '등푸른 생선, 퓨린 다량', '주 1회 이내 소량', 'ACR 2020 / USDA'),
('꽁치', 'Pacific Saury', '해산물', 183.0, 'HIGH', 'BAD', '등푸른 생선 공통 주의', '-', 'ACR 2020 / 일본 식품성분표'),
('참치(다랑어)', 'Tuna', '해산물', 157.0, 'HIGH', 'BAD', '붉은살 생선류 제한', '통조림도 유사', 'ACR 2020'),
('새우', 'Shrimp', '해산물', 273.0, 'HIGH', 'BAD', '갑각류는 퓨린 높음', '소량 제한', 'ACR 2020 / USDA'),
-- ============ MEDIUM / MODERATE (중간) ============
('소고기(살코기)', 'Beef Lean', '육류', 110.0, 'MEDIUM', 'MODERATE', '살코기는 중간 수준', '1일 100g 이내 권장', 'ACR 2020 / Choi HK NEJM 2004 350:1093'),
('돼지고기(살코기)', 'Pork Lean', '육류', 120.0, 'MEDIUM', 'MODERATE', '살코기는 중간 수준', '1일 100g 이내 권장', 'ACR 2020 / Choi HK NEJM 2004'),
('닭가슴살', 'Chicken Breast', '육류', 140.0, 'MEDIUM', 'MODERATE', '붉은 고기보다는 선호', '내장/껍질은 피할 것', 'ACR 2020'),
('연어', 'Salmon', '해산물', 120.0, 'MEDIUM', 'MODERATE', '오메가3 이점 있음, 적정량 허용', '주 1-2회', 'ACR 2020 / AHA 권고'),
('시금치', 'Spinach', '채소', 57.0, 'MEDIUM', 'GOOD', '채소 퓨린은 통풍 위험과 관련성 낮음', '걱정 없이 섭취 가능', 'Choi HK, NEJM 2004 350:1093 - 채소 퓨린 무관'),
('아스파라거스', 'Asparagus', '채소', 23.0, 'LOW', 'GOOD', '실측 퓨린은 낮으며 채소는 안전', '제한 불필요', 'Choi HK, NEJM 2004'),
('콩류(대두)', 'Soybean', '콩류', 172.0, 'HIGH', 'MODERATE', '식물성 퓨린이나 임상 영향은 낮음', '적정량 권장', 'Choi HK, NEJM 2004 / Teng GG, Arthritis Rheum 2015'),
-- ============ LOW / GOOD (권장) ============
('계란', 'Egg', '난류', 2.0, 'LOW', 'GOOD', '퓨린 매우 낮음, 단백질 공급원', '제한 없음', 'ACR 2020 권장 단백질원'),
('우유', 'Milk', '유제품', 4.0, 'LOW', 'GOOD', '저지방 유제품은 요산 감소 효과', '하루 1-2잔 권장', 'Choi HK, NEJM 2004 350:1093 - 유제품 역상관'),
('저지방 우유', 'Low-fat Milk', '유제품', 4.0, 'LOW', 'GOOD', '통풍 위험 감소 효과 근거 명확', '1일 2잔 권장', 'Choi HK et al., NEJM 2004'),
('요거트', 'Yogurt', '유제품', 4.0, 'LOW', 'GOOD', '저지방/무가당이 이상적', '가당 요거트는 주의(과당)', 'Choi HK, NEJM 2004'),
('치즈', 'Cheese', '유제품', 8.0, 'LOW', 'GOOD', '유제품군 안전', '나트륨은 별도 주의', 'ACR 2020'),
('쌀밥', 'Rice', '곡류', 26.0, 'LOW', 'GOOD', '탄수화물 주식, 안전', '제한 없음', 'USDA / ACR 2020'),
('빵(식빵)', 'Bread', '곡류', 29.0, 'LOW', 'GOOD', '정제 곡물 주식', '과당 토핑 주의', 'USDA'),
('감자', 'Potato', '채소', 16.0, 'LOW', 'GOOD', '전분질 채소, 안전', '-', 'USDA'),
('당근', 'Carrot', '채소', 8.0, 'LOW', 'GOOD', '저퓨린 채소', '-', 'USDA'),
('양배추', 'Cabbage', '채소', 22.0, 'LOW', 'GOOD', '저퓨린 채소', '-', 'USDA'),
('사과', 'Apple', '과일', 14.0, 'LOW', 'GOOD', '저퓨린 과일. 과당은 과량 주의', '하루 1-2개 무난', 'Choi HK, BMJ 2008'),
('바나나', 'Banana', '과일', 57.0, 'MEDIUM', 'GOOD', '퓨린 측정값 다양하나 임상 위험 낮음', '-', 'USDA'),
('체리', 'Cherry', '과일', 7.0, 'LOW', 'GOOD', '통풍 발작 위험 35% 감소 보고', '하루 10-12개 권장', 'Zhang Y, Arthritis Rheum 2012 64:4004'),
('커피', 'Coffee', '음료', 0.0, 'LOW', 'GOOD', '요산 감소 및 통풍 위험 감소 근거', '하루 2-4잔 권장', 'Choi HK, Arthritis Rheum 2007 57:816'),
('물', 'Water', '음료', 0.0, 'LOW', 'GOOD', '수분 섭취는 요산 배설 촉진', '하루 2L 이상 권장', 'ACR 2020 가이드라인'),
('토마토', 'Tomato', '채소', 11.0, 'LOW', 'GOOD', '저퓨린 채소', '일부 환자는 발작 유발 호소 - 개별 주의', 'USDA / Flynn TJ, BMC Musculoskelet Disord 2015');
