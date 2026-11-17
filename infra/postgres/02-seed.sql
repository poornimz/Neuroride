INSERT INTO users (id, email, full_name, phone, role, rating)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'rider@neuroride.ai', 'Alex Rider', '+14155550101', 'RIDER', 4.95),
  ('22222222-2222-2222-2222-222222222222', 'driver@neuroride.ai', 'Sam Driver', '+14155550102', 'DRIVER', 4.98)
ON CONFLICT (id) DO NOTHING;

INSERT INTO driver_profiles (user_id, vehicle_make, vehicle_model, license_plate, status)
VALUES
  ('22222222-2222-2222-2222-222222222222', 'Tesla', 'Model 3', 'NR-2025-AI', 'AVAILABLE')
ON CONFLICT (user_id) DO NOTHING;