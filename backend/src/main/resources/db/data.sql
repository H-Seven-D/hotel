USE homestay_booking;

INSERT IGNORE INTO sys_user
    (id, username, password, real_name, phone, role, status)
VALUES
    (1, 'admin', 'Admin@123', '系统管理员', '13800000001', 'ADMIN', 1),
    (2, 'employee', 'Staff@123', '前台员工', '13800000002', 'EMPLOYEE', 1),
    (3, 'zhangmin', 'Staff@123', '张敏', '13800000003', 'EMPLOYEE', 1);

INSERT IGNORE INTO property
    (id, name, type, address, phone, description, status)
VALUES
    (1, '云栖山居民宿', 'HOMESTAY', '浙江省杭州市西湖区梅灵南路 18 号', '0571-88001001',
     '临近茶园与山谷步道的精品民宿，提供家庭房和观景房。', 'ACTIVE'),
    (2, '城市精选酒店', 'HOTEL', '浙江省杭州市拱墅区湖墅南路 66 号', '0571-88001002',
     '面向商务与短途出行客人的城市酒店。', 'ACTIVE');

INSERT IGNORE INTO room_type
    (id, property_id, name, capacity, base_price, description, status)
VALUES
    (1, 1, '山景大床房', 2, 428.00, '落地窗山景，含双人早餐。', 'ACTIVE'),
    (2, 1, '亲子家庭房', 4, 568.00, '一大一小床布局，适合家庭出行。', 'ACTIVE'),
    (3, 2, '商务大床房', 2, 328.00, '独立办公区，适合商务出行。', 'ACTIVE'),
    (4, 2, '双床标准间', 2, 298.00, '两张单人床，含双人早餐。', 'ACTIVE');

INSERT IGNORE INTO room
    (id, property_id, room_type_id, room_no, floor, status, notes)
VALUES
    (1, 1, 1, 'A101', 1, 'AVAILABLE', '靠近庭院'),
    (2, 1, 1, 'A102', 1, 'RESERVED', '山景方向'),
    (3, 1, 2, 'A201', 2, 'OCCUPIED', '家庭楼层'),
    (4, 1, 2, 'A202', 2, 'CLEANING', '退房清洁中'),
    (5, 2, 3, 'B501', 5, 'AVAILABLE', '安静楼层'),
    (6, 2, 3, 'B502', 5, 'MAINTENANCE', '空调检修'),
    (7, 2, 4, 'B503', 5, 'AVAILABLE', '靠近电梯'),
    (8, 2, 4, 'B504', 5, 'AVAILABLE', '无烟房');

INSERT IGNORE INTO customer
    (id, name, phone, id_card, email, remark)
VALUES
    (1, '陈雨', '13900001001', '330106199201010011', 'chenyu@example.com', '偏好安静房间'),
    (2, '林舟', '13900001002', '330105199408080022', 'linzhou@example.com', '常客'),
    (3, '周可', '13900001003', '330108199612120033', 'zhouke@example.com', '需要儿童用品');

INSERT IGNORE INTO booking
    (id, booking_no, property_id, room_id, customer_id, check_in_date, check_out_date,
     guest_count, total_amount, status, remark, created_by)
VALUES
    (1, 'BK202611190001', 1, 3, 3, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     3, 1136.00, 'CHECKED_IN', '家庭出行，需要加一张儿童床', 2),
    (2, 'BK202611190002', 1, 2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),
     DATE_ADD(CURDATE(), INTERVAL 3 DAY), 2, 856.00, 'CONFIRMED', '预计晚间到店', 2),
    (3, 'BK202611190003', 2, 5, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     DATE_ADD(CURDATE(), INTERVAL 4 DAY), 1, 656.00, 'CONFIRMED', '商务出差', 1),
    (4, 'BK202611180004', 2, 7, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY),
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2, 298.00, 'COMPLETED', '已完成入住', 2);

INSERT IGNORE INTO booking
    (booking_no, property_id, room_id, customer_id, check_in_date, check_out_date,
     guest_count, total_amount, status, remark, created_by, created_at)
VALUES
    ('DEMO-D13-A', 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 14 DAY),
     DATE_SUB(CURDATE(), INTERVAL 13 DAY), 2, 428.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ('DEMO-D13-B', 2, 7, 2, DATE_SUB(CURDATE(), INTERVAL 14 DAY),
     DATE_SUB(CURDATE(), INTERVAL 13 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ('DEMO-D12-A', 1, 2, 3, DATE_SUB(CURDATE(), INTERVAL 13 DAY),
     DATE_SUB(CURDATE(), INTERVAL 12 DAY), 3, 568.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 12 DAY)),
    ('DEMO-D12-B', 2, 8, 1, DATE_SUB(CURDATE(), INTERVAL 13 DAY),
     DATE_SUB(CURDATE(), INTERVAL 12 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 12 DAY)),
    ('DEMO-D11-A', 2, 5, 2, DATE_SUB(CURDATE(), INTERVAL 12 DAY),
     DATE_SUB(CURDATE(), INTERVAL 11 DAY), 1, 328.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 11 DAY)),
    ('DEMO-D10-A', 2, 7, 3, DATE_SUB(CURDATE(), INTERVAL 11 DAY),
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('DEMO-D10-B', 1, 1, 2, DATE_SUB(CURDATE(), INTERVAL 11 DAY),
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2, 428.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('DEMO-D9-A', 2, 8, 1, DATE_SUB(CURDATE(), INTERVAL 10 DAY),
     DATE_SUB(CURDATE(), INTERVAL 9 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 9 DAY)),
    ('DEMO-D8-A', 1, 2, 3, DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     DATE_SUB(CURDATE(), INTERVAL 8 DAY), 3, 568.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('DEMO-D8-B', 2, 5, 1, DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     DATE_SUB(CURDATE(), INTERVAL 8 DAY), 1, 328.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('DEMO-D7-A', 1, 1, 2, DATE_SUB(CURDATE(), INTERVAL 8 DAY),
     DATE_SUB(CURDATE(), INTERVAL 7 DAY), 2, 428.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 7 DAY)),
    ('DEMO-D6-A', 2, 7, 3, DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     DATE_SUB(CURDATE(), INTERVAL 6 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 6 DAY)),
    ('DEMO-D6-B', 1, 2, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     DATE_SUB(CURDATE(), INTERVAL 6 DAY), 3, 568.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 6 DAY)),
    ('DEMO-D5-A', 2, 8, 2, DATE_SUB(CURDATE(), INTERVAL 6 DAY),
     DATE_SUB(CURDATE(), INTERVAL 5 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 5 DAY)),
    ('DEMO-D4-A', 2, 5, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY),
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1, 328.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('DEMO-D4-B', 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY),
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), 2, 428.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 4 DAY)),
    ('DEMO-D3-A', 2, 7, 2, DATE_SUB(CURDATE(), INTERVAL 4 DAY),
     DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 3 DAY)),
    ('DEMO-D2-A', 2, 8, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY),
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 298.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('DEMO-D2-B', 1, 2, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY),
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3, 568.00, 'COMPLETED', '两周趋势演示订单', 2,
     DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('DEMO-D1-A', 2, 5, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY),
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 328.00, 'COMPLETED', '两周趋势演示订单', 1,
     DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('DEMO-D0-A', 1, 1, 3, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     2, 856.00, 'CHECKED_IN', '近日入住演示订单', 2, NOW()),
    ('DEMO-D0-B', 2, 7, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),
     DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, 298.00, 'CONFIRMED', '近日预订演示订单', 2, NOW());

INSERT IGNORE INTO room_status_log
    (id, room_id, room_no, before_status, after_status, operator_id, operator_name, notes)
VALUES
    (1, 3, 'A201', 'RESERVED', 'OCCUPIED', 2, '前台员工', '客户已到店办理入住'),
    (2, 4, 'A202', 'OCCUPIED', 'CLEANING', 2, '前台员工', '客户退房，安排清洁'),
    (3, 6, 'B502', 'AVAILABLE', 'MAINTENANCE', 1, '系统管理员', '空调设备检修');

INSERT IGNORE INTO employee_schedule
    (id, user_id, property_id, work_date, shift, status, remark)
VALUES
    (1, 2, 1, CURDATE(), 'MORNING', 'COMPLETED', '负责前台接待'),
    (2, 3, 1, CURDATE(), 'AFTERNOON', 'SCHEDULED', '负责入住与客房协调'),
    (3, 2, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MORNING', 'SCHEDULED', '城市酒店前台'),
    (4, 3, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'NIGHT', 'SCHEDULED', '夜间值守');
