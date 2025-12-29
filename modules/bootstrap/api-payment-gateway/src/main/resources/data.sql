-- Test data for payment gateway
-- Insert test partners
INSERT INTO partner (id, code, name, active) VALUES (1, 'MOCK_PARTNER', 'Mock Partner', true);
INSERT INTO partner (id, code, name, active) VALUES (2, 'TESTPG_PARTNER', 'TestPG Partner', true);
INSERT INTO partner (id, code, name, active) VALUES (3, 'TOSS_PARTNER', 'Toss Payments Partner', true);

-- Insert fee policies for partners
INSERT INTO partner_fee_policy (partner_id, effective_from, percentage, fixed_fee)
VALUES (1, '2024-01-01 00:00:00', 0.025000, 100);

INSERT INTO partner_fee_policy (partner_id, effective_from, percentage, fixed_fee)
VALUES (2, '2024-01-01 00:00:00', 0.023500, 100);

INSERT INTO partner_fee_policy (partner_id, effective_from, percentage, fixed_fee)
VALUES (3, '2024-01-01 00:00:00', 0.020000, 100);
