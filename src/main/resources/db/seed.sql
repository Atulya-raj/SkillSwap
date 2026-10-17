INSERT INTO categories (name, description) VALUES
 ('Programming','Languages, tools and engineering skills'),
 ('Design','UI/UX, graphics and visual design'),
 ('Music','Instruments, theory and production'),
 ('Languages','Spoken and written languages'),
 ('Academics','Course subjects and exam preparation'),
 ('Sports & Fitness','Sports coaching and fitness'),
 ('Creative Arts','Photography, video, writing and art'),
 ('Career & Soft Skills','Interviews, resumes, public speaking');

INSERT INTO skills (category_id, name) VALUES
 (1,'Java'),(1,'Python'),(1,'Data Structures & Algorithms'),(1,'Web Development'),(1,'SQL & Databases'),
 (2,'Figma / UI Design'),(2,'Graphic Design'),(2,'Logo & Branding'),
 (3,'Guitar'),(3,'Keyboard / Piano'),(3,'Music Production'),
 (4,'English Communication'),(4,'Hindi'),(4,'Tamil'),(4,'French'),(4,'Japanese'),
 (5,'Calculus'),(5,'Digital Logic'),(5,'Engineering Economics'),(5,'Physics'),
 (6,'Badminton'),(6,'Chess'),(6,'Gym Training'),
 (7,'Photography'),(7,'Video Editing'),(7,'Creative Writing'),
 (8,'Resume Review'),(8,'Mock Interviews'),(8,'Public Speaking');

INSERT INTO badges (code, name, description) VALUES
 ('FIRST_SWAP','First Swap','Completed your first session'),
 ('MENTOR_10','Mentor','Taught 10 sessions with a 4.5+ average rating'),
 ('ETERNAL_LEARNER','Eternal Learner','Learned in 10 sessions'),
 ('PERFECT_FIVE','Perfect Five','Received five 5-star reviews'),
 ('CONNECTOR','Connector','Offering 3 or more skills'),
 ('RELIABLE','Reliable','5 completed sessions with no strikes');
