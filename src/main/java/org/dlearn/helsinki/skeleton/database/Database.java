  package org.dlearn.helsinki.skeleton.database;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dlearn.helsinki.skeleton.exceptions.GroupUpdateUnsuccessful;
import org.dlearn.helsinki.skeleton.model.Answer;
import org.dlearn.helsinki.skeleton.model.ChangePasswordStudent;
import org.dlearn.helsinki.skeleton.model.ClassThemeAverage;
import org.dlearn.helsinki.skeleton.model.Classes;
import org.dlearn.helsinki.skeleton.model.Group;
import org.dlearn.helsinki.skeleton.model.NewStudent;
import org.dlearn.helsinki.skeleton.model.NewTeacher;
import org.dlearn.helsinki.skeleton.model.GroupThemeAverage;
import org.dlearn.helsinki.skeleton.model.ListClassThemeAverage;
import org.dlearn.helsinki.skeleton.model.ListGroupThemeAverage;
import org.dlearn.helsinki.skeleton.model.ListStudentThemeAverage;
import org.dlearn.helsinki.skeleton.model.Question;
import org.dlearn.helsinki.skeleton.model.Researcher;
import org.dlearn.helsinki.skeleton.model.Student;
import org.dlearn.helsinki.skeleton.model.StudentGroup;
import org.dlearn.helsinki.skeleton.model.StudentThemeAverage;
import org.dlearn.helsinki.skeleton.model.Survey;
import org.dlearn.helsinki.skeleton.model.SurveyTheme;
import org.dlearn.helsinki.skeleton.model.Teacher;
import org.dlearn.helsinki.skeleton.security.Hasher;


public class Database {
    
    private static final Logger log = LogManager.getLogger(Database.class);
    
    private static final BasicDataSource DATA_SOURCE = new BasicDataSource();
    private static final String DB_DRIVER = "org.postgresql.Driver";
    
    private static final String DEV_DB_CONNECTION = "jdbc:postgresql://localhost:5432/Dlearn_db?verifyServerCertificate=false&useSSL=true&useServerPrepStmts=false&rewriteBatchedStatements=true";
    private static final String DEV_DB_USER = "postgres";
    private static final String DEV_DB_PASSWORD = "admin";
    
    static {
        try {
            Class.forName(DB_DRIVER);
            DATA_SOURCE.setDriverClassName(DB_DRIVER);
            DATA_SOURCE.setInitialSize(1);
            String dbUrl = System.getenv("JDBC_DATABASE_URL");
            if (dbUrl == null) {
            	System.out.println("Hello");
                dbUrl = DEV_DB_CONNECTION;
                DATA_SOURCE.setUsername(DEV_DB_USER);
                DATA_SOURCE.setPassword(DEV_DB_PASSWORD);
            }else{
	            if (dbUrl.isEmpty()) {
	                String databaseUrl = System.getenv("DATABASE_URL");
	                if (databaseUrl.isEmpty()) {
	                    dbUrl = DEV_DB_CONNECTION;
	                    DATA_SOURCE.setUsername(DEV_DB_USER);
	                    DATA_SOURCE.setPassword(DEV_DB_PASSWORD);
	                } else {
	                    URI dbUri = new URI(databaseUrl);
	                    dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();
	                    if (dbUri.getUserInfo() != null) {
	                        DATA_SOURCE.setUsername(dbUri.getUserInfo().split(":")[0]);
	                        DATA_SOURCE.setPassword(dbUri.getUserInfo().split(":")[1]);
	                    }
	                }
	            }
            }
            DATA_SOURCE.setUrl(dbUrl);
        } catch (URISyntaxException | ClassNotFoundException e) {
            log.catching(Level.FATAL, e);
        }
    }

    public void testConnection() throws Exception {
        log.traceEntry("Testing connection");
        try (Connection dbConnection = getDBConnection()) {
            log.debug("Database connection succeeded.");
        } catch (Exception e) {
            log.catching(e);
        }
        log.traceExit();
    }

    // Survey postSurvey : returns the survey that was posted on the database.
    public SurveyTheme postSurvey(SurveyTheme surveyTheme) throws SQLException {
        log.traceEntry("Posting survey {}", surveyTheme);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Surveys\" (title, title_fi, class_id, start_date, teacher_id, description, description_fi, open) "
                    + "VALUES (?,?,?,now(),?,?,?,True) RETURNING _id";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, surveyTheme.title);
                insert.setString(2, surveyTheme.title_fi);
                insert.setInt(3, surveyTheme.getClass_id());
                //insert.setDate(3, new Date(0));
                insert.setInt(4, surveyTheme.getTeacher_id());
                insert.setString(5, surveyTheme.description);
                insert.setString(6, surveyTheme.description_fi);
                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                    	surveyTheme.set_id(result.getInt("_id"));
                    } else {
                        log.error("Inserting survey didn't return ID of it.");
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(surveyTheme);
        return surveyTheme;
    }

    public List<Question> getQuestions(SurveyTheme surveyTheme) {
        log.traceEntry();
        ArrayList<Question> questions = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
        	// lame fix TODO check that there is at least one theme chosen
            String statement = "Select * FROM public.\"Questions\" WHERE theme_id = ?";
            int count = 1;
            for(int i=1;i<surveyTheme.theme_ids.size();i++){
            	statement += " OR theme_id = ?";
            	count++;
            	System.out.println(count);
            }
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
            	count = 0;
                for(int i=0;i<surveyTheme.theme_ids.size();i++){
                	count++;
                	System.out.println(count);
                	select.setInt(i+1, surveyTheme.theme_ids.get(i));
                }
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Question question = new Question();
                        question.setQuestion(result.getString(1));
                        question.setMin_answer(result.getInt(2));
                        question.setMax_answer(result.getInt(3));
                        question.set_id(result.getInt(4));
                        questions.add(question);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(questions);
        return questions;
    }

    public Question postQuestion(Question question) throws SQLException {
        log.traceEntry("Posting question {}", question);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Questions\" (question, question_fi, min_answer, max_answer) "
                    + "VALUES (?, ?, ?, ?) RETURNING _id";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, question.question);
                insert.setString(2, question.question_fi);
                insert.setInt(3, question.min_answer);
                insert.setInt(4, question.max_answer);
                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                        question.set_id(result.getInt("_id"));
                    } else {
                        log.error("Inserting question didn't return ID of it.");
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(question);
        return question;
    }

    public void postSurveyQuestions(List<Question> questions, Survey survey) {
        log.traceEntry("Posting questions {} for survey {}", questions, survey);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Survey_questions\" (survey_id, question_id) "
                    + "VALUES (?,?)";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                // prepare batch
                for (Question question : questions) {
                    insert.setInt(1, survey.get_id());
                    insert.setInt(2, question.get_id());
                    insert.addBatch();
                }
                // execute query
                insert.executeBatch();
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit();
    }

    // Method getQuestionFromSurvey
    // parameter : int, id of survey
    // output : ArrayList<Question>, list of questions from the survey
    // Takes a survey id and returns all the questions set to that survey
    public List<Question> getQuestionsFromSurvey(int survey_id) {
        log.traceEntry("Getting the questions from the survey {}", survey_id);
        ArrayList<Question> questions = new ArrayList<>();

        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "Select _id, question, question_fi, min_answer, max_answer FROM \"Questions\", \"Survey_questions\" WHERE"
                    + " \"Survey_questions\".survey_id = ? AND \"Survey_questions\".question_id = \"Questions\"._id";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, survey_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Question question = new Question();
                        question.set_id(result.getInt(1));
                        question.setQuestion(result.getString(2));
                        question.setQuestion_fi(result.getString(3));
                        question.setMin_answer(result.getInt(4));
                        question.setMax_answer(result.getInt(5));
                        questions.add(question);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(questions);
        return questions;
    }

    // Method : postSutdentAnswersForSurvey
    // Takes the survey_id, the student_id
    public void postStudentAnswersForSurvey(List<Answer> answers, int survey_id,
            int student_id) {
        log.debug("Posting all answers same time isn't implemented.");
        // TODO implement but currently it's easier for front-end to send one at a time...
    }

    // Method : getSurveysFromClassAsStudent
    // Input  : the survey_id, the student_id
    // Output : returns a list of surveys available to the student
    public List<Survey> getSurveysFromClassAsStudent(int student_id,
            int class_id) throws SQLException {
        log.traceEntry("Getting all surveys from class {} as student {}", class_id, student_id);
    	ArrayList<Survey> surveys = new ArrayList<>();

        try (Connection dbConnection = getDBConnection()) {
                                                //1     2        3           4              5          6        7    8         9
            String statement = "SELECT distinct _id,title,title_fi,description,description_fi,start_date,end_date,open,teacher_id "
                    + "FROM public.\"Surveys\" "
                    + "WHERE class_id = ? ";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Survey survey = new Survey();
                        survey.set_id(result.getInt(1));
                        survey.setTitle(result.getString(2));
                        survey.setTitle_fi(result.getString(3));
                        survey.setDescription(result.getString(4));
                        survey.setDescription_fi(result.getString(5));
                        survey.setStart_date(result.getTimestamp(6));
                        result.getTimestamp(7);
                        if (!result.wasNull()) {
                            survey.setEnd_date(result.getTimestamp(7));
                        }
                        survey.setOpen(result.getBoolean(8));
                        survey.setClass_id(class_id);
                        survey.setTeacher_id(result.getInt(9));
                        surveys.add(survey);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(surveys);
        return surveys;
    }

    // Method : getSurveysFromClassAsStudent
    // Input  : the survey_id, the student_id
    // Output : returns a list of surveys available to the student
    public List<Survey> getSurveysFromClassAsTeacher(int teacher_id,
            int class_id) throws SQLException {
        log.traceEntry("Getting surveys from class {} as student {}", class_id, teacher_id);
        ArrayList<Survey> surveys = new ArrayList<>();

        try (Connection dbConnection = getDBConnection()) {
            String statement = "SELECT _id,title, title_fi, description, description_fi, start_date,end_date,open "
                    + "FROM public.\"Surveys\" "
                    + "WHERE class_id = ? AND teacher_id = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);
                select.setInt(2, teacher_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Survey survey = new Survey();
                        survey.set_id(result.getInt(1));
                        survey.setTitle(result.getString(2));
                        survey.setTitle_fi(result.getString(3));
                        survey.setDescription(result.getString(4));
                        survey.setDescription_fi(result.getString(5));
                        survey.setStart_date(result.getTimestamp(6));
                        
                        if (!result.getBoolean(8)){
                            survey.setEnd_date(result.getTimestamp(7));
                        }
                        survey.setOpen(result.getBoolean(8));
                        survey.setClass_id(class_id);
                        survey.setTeacher_id(teacher_id);
                        surveys.add(survey);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(surveys);
        return surveys;
    }

    public List<Survey> getSurveys() {
        log.traceEntry("Getting all surveys");
        List<Survey> survey = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            String statement = "SELECT _id, title, title_fi, class_id, start_date, end_date, teacher_id, description, description_fi open FROM public.\"Surveys\"";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        survey.add(new Survey() {{
                            this._id = result.getInt("_id");
                            this.title = result.getString("title");
                            this.title_fi = result.getString("title_fi");
                            this.class_id = result.getInt("class_id");
                            this.start_date = result.getTimestamp("start_date");
                            this.end_date = result.getTimestamp("end_date");
                            this.teacher_id = result.getInt("teacher_id");
                            this.description = result.getString("description");
                            this.description_fi = result.getString("description_fi");
                            this.open = result.getBoolean("open");
                        }});
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(survey);
        return survey;
    }
    
    public Optional<Group> getGroupForStudent(int class_id, int student_id) {
        log.traceEntry("Getting groups for student {} in class {}", student_id, class_id);
        try (Connection dbConnection = getDBConnection()) {
            String statement = ""
                    + "SELECT sc.group_id, g.name, g.open "
                    + "  FROM public.\"Student_Classes\" as sc,"
                    + "       public.\"Groups\" as g"
                    + "  WHERE sc.group_id = g._id"
                    + "    AND sc.class_id = g.class_id"
                    + "    AND sc.student_id = ?"
                    + "    AND sc.class_id = ?"
                    + "  ORDER BY sc.creation_date DESC"
                    + "  LIMIT 1";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, student_id);
                select.setInt(2, class_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Group group = new Group() {{
                            this.set_id(result.getInt("group_id"));
                            this.setClass_id(class_id);
                            this.setName(result.getString("name"));
                            this.setOpen(result.getBoolean("open"));
                        }};
                        log.traceExit(group);
                        return Optional.of(group);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit("No group");
        return Optional.empty();
    }

    public List<Group> getAllGroupsForStudent(int student_id) {
        log.traceEntry("Getting all groups for student {}", student_id);
        ArrayList<Group> groups = new ArrayList<>();

        try (Connection dbConnection = getDBConnection()) {
            String statement = ""
                    + "SELECT sc.group_id, g.name, g.class_id, g.open "
                    + "  FROM public.\"Student_Classes\" as sc,"
                    + "       public.\"Groups\" as g"
                    + "  WHERE sc.group_id = g._id"
                    + "    AND sc.class_id = g.class_id"
                    + "    AND sc.student_id = ?"
                    + "    AND sc.class_id = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, student_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Group group = new Group();
                        group.set_id(result.getInt("group_id"));
                        group.setClass_id(result.getInt("class_id"));
                        group.setName(result.getString("name"));
                        group.setOpen(result.getBoolean("open"));
                        groups.add(group);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(groups);
        return groups;
    }
    
    // TODO: Use optional
    public List<Group> getAllGroupsFromClass(int class_id, boolean all) {
        log.traceEntry("Getting groups for the class {}", class_id);
        ArrayList<Group> groups = null;

        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select name, _id, class_id, open "
            		+ "FROM public.\"Groups\" "
            		+ "WHERE (class_id = ?) ";
            		if(!all)
            			statement += "AND (open = true)";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    groups = new ArrayList<>();
                    while (result.next()) {
                        Group group = new Group();
                        group.set_id(result.getInt("_id"));
                        group.setClass_id(result.getInt("class_id"));
                        group.setName(result.getString("name"));
                        group.setOpen(result.getBoolean("open"));
                        groups.add(group);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(groups);
        return groups;
    }

    // TODO: Use optional
    public Group getGroupFromClass(int class_id, int group_id) {
        log.traceEntry("Getting group class {}", class_id);
        Group group = null;

        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select name, open FROM public.\"Groups\" WHERE (class_id = ?) and (_id = ?);";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);
                select.setInt(2, group_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        group = new Group(group_id, result.getString("name"),
                                class_id, result.getBoolean("open"));
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(group);
        return group;
    }

    public List<Student> getAllStudentsFromClassAndGroup(int class_id,
            int group_id) {
        log.traceEntry("Getting groups for the class {}", group_id);
        ArrayList<Student> students = null;

        try (Connection dbConnection = getDBConnection()) {
            // TODO update request to remove class_id
            String statement = ""
                    + "SELECT s._id, s.username, s.gender, s.age\n"
                    + "  FROM public.\"Student_Classes\" as sc,\n"
                    + "       public.\"Students\" as s \n"
                    + " WHERE sc.student_id = s._id\n"
                    + "   AND sc.class_id = ?\n"
                    + "   AND sc.group_id = ?\n"
                    + "   AND sc._id = (SELECT sc2._id\n"
                    + "                  FROM public.\"Student_Classes\" as sc2\n"
                    + "                  WHERE sc.student_id = sc2.student_id\n"
                    + "                 ORDER BY sc2.creation_date DESC\n"
                    + "                 LIMIT 1)";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);
                select.setInt(2, group_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    students = new ArrayList<>();
                    while (result.next()) {
                        Student student = new Student(result.getInt("_id"),
                                result.getString("username"),
                                result.getString("gender"),
                                result.getInt("age"));
                        students.add(student);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(students);
        return students;
    }

    public Optional<Student> createStudent(NewStudent new_student) {
        log.traceEntry("Creating student {}", new_student);
        Optional<Student> student = Optional.empty();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Students\" (username, pwd, gender, age) "
                    + "VALUES (?,?,?,?) RETURNING _id";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, new_student.student.username);
                insert.setString(2,
                        Hasher.getHasher().encode(new_student.password));
                insert.setString(3, new_student.student.gender);
                insert.setInt(4, new_student.student.age);

                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                        new_student.student._id = result.getInt("_id");
                        student = Optional.of(new_student.student);
                    } else {
                        log.error("Inserting student didn't return ID of it.");
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(student);
        return student;
    }

    public Optional<Teacher> createTeacher(NewTeacher new_teacher) {
        log.traceEntry("Creating new teacher {}", new_teacher);
        Optional<Teacher> teacher = Optional.empty();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Teachers\" (username, pwd) "
                    + "VALUES (?,?) RETURNING _id";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, new_teacher.teacher.username);
                insert.setString(2,
                        Hasher.getHasher().encode(new_teacher.password));

                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                        new_teacher.teacher._id = result.getInt("_id");
                    } else {
                        log.error("Inserting teacher didn't return ID of it.");
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        return teacher;
    }
    
    public Optional<Student> changeStudentPassword(ChangePasswordStudent student) {
        log.traceEntry("Changing students password {}", student);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "UPDATE public.\"Students\" SET pwd = ? WHERE _id = ?";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1,
                        Hasher.getHasher().encode(student.password));
                insert.setInt(2, student.student_id);

                // execute query
                insert.execute();
            }
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No password change");
            return Optional.empty();
        }
        Student result = getStudent(student.student_id);
        log.traceExit(result);
        return Optional.ofNullable(result);
    }

    public boolean addStudentToGroup(Student student, int class_id, int group_id) {
        log.traceEntry("Adding student {} to group {} in class {}", student, group_id, class_id);
        try (Connection dbConnection = getDBConnection()) {
            if (!DataBaseHelper.doesGroupClassMatch(dbConnection, group_id, class_id)) {
                log.traceExit("Group and class didn't match");
                return false;
            }
            String statement = "INSERT INTO public.\"Student_Classes\" (student_id, class_id, group_id) "
                    + "VALUES (?,?,?)";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setInt(1, student._id);
                insert.setInt(2, class_id);
                insert.setInt(3, group_id);

                // execute query
                insert.executeUpdate();
                log.traceExit("Adding succesful");
                return true;
            }
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("Adding failed");
            return false;
        }
    }

    // TODO: Use optional
    public Student getStudent(int studentID) {
        log.traceEntry("Getting student {}", studentID);
        Student student = null;

        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select username, gender, age FROM public.\"Students\" WHERE _id = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, studentID);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        student = new Student();
                        student.set_id(studentID);
                        student.setAge(result.getInt("age"));
                        student.setUsername(result.getString("username"));
                        student.setGender(result.getString("gender"));
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(student);
        return student;
    }
    
    public Optional<List<Student>> getAllStudentsFromClass(int class_id) {
        log.traceEntry("Getting all students from class {}", class_id);
        try {
            List<Student> result = DataBaseHelper.query(Database::getDBConnection, ""
                    + "SELECT st._id,\n"
                    + "       st.username,\n"
                    + "       st.gender,\n"
                    + "       st.age\n"
                    + "  FROM public.\"Groups\" AS gr,\n"
                    + "       public.\"Student_Classes\" AS sc,\n"
                    + "       public.\"Students\" AS st\n"
                    + " WHERE st._id = sc.student_id\n"
                    + "   AND gr._id = sc.group_id\n"
                    + "   AND gr.class_id = ?\n"
                    + "   AND sc._id = (SELECT sc2._id\n"
                    + "                   FROM public.\"Student_Classes\" as sc2\n"
                    + "                  WHERE sc.student_id = sc2.student_id\n"
                    + "                 ORDER BY sc2.creation_date DESC\n"
                    + "                 LIMIT 1)"
                    + "ORDER BY st.username",
                    select -> select.setInt(1, class_id),
                    results -> {
                        List<Student> students = new ArrayList<>();
                        for (ResultSet r: results) {
                            students.add(new Student() {{
                                _id = r.getInt("_id");
                                age = r.getInt("age");
                                username = r.getString("username");
                                gender = r.getString("gender");
                            }});
                        }
                        return students;
                    }
            );
            log.traceExit(result);
            return Optional.of(result);
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No students returned");
            return Optional.empty();
        }
    }

    public Optional<Student> getStudentFromUsername(String username_) {
        log.traceEntry("Getting student from the username {}", username_);
        Optional<Student> student = Optional.empty();
        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select _id, gender, age FROM public.\"Students\" WHERE username = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setString(1, username_);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        student = Optional.of(new Student() {{
                            this.username = username_;
                            _id = result.getInt("_id");
                            age = result.getInt("age");
                            gender = result.getString("gender");
                        }});
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(student);
        return student;
    }

    public Optional<Teacher> getTeacherFromUsername(String username_) {
        log.traceEntry("Getting teacher from username {}", username_);
        Optional<Teacher> teacher = Optional.empty();
        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select _id FROM public.\"Teachers\" WHERE username = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setString(1, username_);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        teacher = Optional.of(new Teacher() {{
                            this.username = username_;
                            _id = result.getInt("_id");
                        }});
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(teacher);
        return teacher;
    }

    public Optional<Researcher> getResearcherFromUsername(String username_) {
        log.traceEntry("Getting researcher from username {}", username_);
        Optional<Researcher> researcher = Optional.empty();
        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select _id FROM public.\"Researchers\" WHERE username = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setString(1, username_);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        researcher = Optional.of(new Researcher() {{
                            this.username = username_;
                            id = result.getInt("_id");
                        }});
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(researcher);
        return researcher;
    }
    
    public boolean doesStudentUsernameExistInDatabase(Student student) {
        log.traceEntry("Checking if student {} exists in database", student);
        boolean exists = false;
        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select username FROM public.\"Students\" as std WHERE std.username = ?";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection.
                    prepareStatement(statement)) {
                select.setString(1, student.getUsername());
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    exists = result.next();
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(exists);
        return exists;
    }

    // TODO: Use optional
    public List<Classes> getAllClassesOfTeacher(int teacher_id) {
        log.traceEntry("Getting all classes of teacher {}", teacher_id);
        List<Classes> classes = null;

        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select _id, name, name_fi "
                    + "FROM public.\"Classes\" as cls "
                    + "WHERE (cls.teacher_id = ?);";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection.prepareStatement(statement)) {
                select.setInt(1, teacher_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    classes = new ArrayList<>();
                    while (result.next()) {
                        Classes newClass = new Classes();
                        newClass.set_id(result.getInt("_id"));
                        newClass.setName(result.getString("name"));
                        newClass.setName_fi(result.getString("name_fi"));
                        newClass.setTeacher_id(teacher_id);
                        classes.add(newClass);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(classes);
        return classes;
    }

    // TODO: Use optional
    public List<Classes> getAllClassesStundentIsIn(int student_id) {
        log.traceEntry("Getting all classes student {} is in", student_id);
        List<Classes> classes = null;

        try (Connection dbConnection = getDBConnection()) {
            String statement = "Select cls._id, cls.name, cls.teacher_id "
                    + "FROM public.\"Groups\" as gr "
                    + "INNER JOIN public.\"Student_Classes\" as st_cls "
                    + "ON st_cls.group_id = gr._id "
                    + "INNER JOIN public.\"Classes\" as cls "
                    + "ON cls._id = gr.class_id "
                    + "WHERE (st_cls.student_id = ?);";
            //prepare statement with student_id
            try (PreparedStatement select = dbConnection.prepareStatement(statement)) {
                select.setInt(1, student_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    classes = new ArrayList<>();
                    while (result.next()) {
                        Classes newClass = new Classes();
                        newClass.set_id(result.getInt("_id"));
                        newClass.setName(result.getString("name"));
                        newClass.setTeacher_id(result.getInt("teacher_id"));
                        classes.add(newClass);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(classes);
        return classes;
    }
	
	public void closeGroup(int group_id) {
		try(Connection dbConnection = getDBConnection()) {
	        String update_statement = "UPDATE public.\"Groups\" "
	        						+ "SET open = false " 
	        						+ "WHERE (_id = ?);" ;
	        try(PreparedStatement update = dbConnection.
	        		prepareStatement(update_statement)) {
	        	update.setInt(1, group_id);
	            // execute query
	            update.executeUpdate();
            };
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
	    }
	}
	
	public boolean doesGroupExistInDatabase(int group_id) {
		boolean exists = false;
		try(Connection dbConnection = getDBConnection()) {
	        String statement = "Select username FROM public.\"Groups\" as std WHERE std.username = ?";
	        //prepare statement with student_id
	        try(PreparedStatement select = dbConnection.
	        		prepareStatement(statement)) {
	        	select.setInt(1, group_id);
	            // execute query
	            try(ResultSet result = select.executeQuery()) {
	            	if(result.next()) {
	            		exists = true;
                	}
                }
            }
	    } catch (SQLException e) {
	    	System.out.println(e.getMessage());
	    }
		return exists;
	}
	

	public void updateGroupName(int class_id, int group_id, Group group) {
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "UPDATE public.\"Groups\" "+ 
            		"SET name = ? WHERE (_id = ?) and (class_id = ?);";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, group.getName());
                insert.setInt(2, group_id);
                insert.setInt(3, class_id);
                // execute query
                int count = insert.executeUpdate();
                if (count == 0) {
                	// update unsuccessful
                	throw new GroupUpdateUnsuccessful();
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }        
	}
	
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
    
    // TODO: Use optional
    private static Connection getDBConnection() {
        try {
            return DATA_SOURCE.getConnection();
        } catch (SQLException e) {
            log.catching(Level.FATAL, e);
            return null;
        }
    }

    // Inserts an answer into the database
    public void putAnswerToQuestion(Answer answer, int class_id) {
        log.traceEntry("Adding answer {}", answer);
        // finding his group_id
        Group group = getGroupForStudent(class_id, answer.student_id).orElse(null);
        int group_id = -1;
        if (group != null){
        	group_id = group._id;
        }
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Answers\" "
                    + "(question_id, student_id, answer, survey_id, group_id)"
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT (question_id,student_id,survey_id) DO UPDATE SET answer = ?";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setInt(1, answer.question_id);
                insert.setInt(2, answer.student_id);
                insert.setInt(3, answer.answer);
                insert.setInt(4, answer.survey_id);
                insert.setInt(5, group_id);
                insert.setInt(6, answer.answer);
                // execute query
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit();
    }

    public List<Answer> getAnswersFromStudentSurvey(int student_id,
            int survey_id) {
        log.traceEntry("Getting answers from student {} survey {}", student_id, survey_id);
        ArrayList<Answer> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "Select student_id, survey_id, question_id, answer FROM \"Answers\" WHERE"
                    + " survey_id = ? AND student_id = ?";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, survey_id);
                select.setInt(2, student_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        Answer answer = new Answer();
                        answer.student_id = result.getInt(1);
                        answer.survey_id = result.getInt(2);
                        answer.setQuestion_id(result.getInt(3));
                        answer.setAnswer(result.getInt(4));
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    }

    public List<GroupThemeAverage> getAverageAnswersFromGroup(int class_id,
            int group_id, int survey_id) {
        log.traceEntry("Getting average answers for survey {} for group {} in class {}", survey_id, group_id, class_id);
        ArrayList<GroupThemeAverage> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "SELECT avg(answer),\"Themes\".title,\"Themes\".title_fi,\"Themes\".description,\"Themes\".description_fi,\"Themes\"._id,\"Surveys\".start_date "
                    + "FROM public.\"Surveys\",public.\"Answers\", public.\"Themes\", public.\"Questions\" "
                    + "WHERE \"Questions\"._id = question_id "
                    + "AND \"Questions\".theme_id = \"Themes\"._id "
                    + "AND \"Surveys\"._id = \"Answers\".survey_id "
                    + "AND \"Answers\".survey_id = ? "
                    + "AND \"Answers\".group_id = ?"
                    + "GROUP BY \"Themes\"._id,start_date";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, survey_id);
                select.setInt(2, group_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        GroupThemeAverage answer = new GroupThemeAverage();
                        answer.setAnswer(result.getFloat(1));
                        answer.setTheme_title(result.getString(2));
                        answer.setTheme_title_fi(result.getString(3));
                        answer.setDescription(result.getString(4));
                        answer.setDescription_fi(result.getString(5));
                        answer.setTheme_id(result.getInt(6));
                        answer.setStart_date(result.getString(7));
                        answer.setGroup_id(group_id);
                        answer.setSurvey_id(survey_id);
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    }

   
    public List<StudentGroup> getGroupsWithStudents(int _class_id, boolean all) {
        log.traceEntry("Getting groups and students in class {}", _class_id);
        Map<Integer, StudentGroup> studentGroups = new TreeMap<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
        	String statement = null;
        	if (all) {
    			statement = "" 
    					// Select all open groups with students 
    	                + "SELECT g._id as group_id,\n" 
    	                + "       g.name as group_name,\n"
    	                + "       g.open as group_open,\n"
    	                + "       s._id as student_id,\n"
    	                + "       s.username,\n"
    	                + "       s.gender,\n"
    	                + "       s.age,\n"
    	                + "		  0 as EmptyStudents\n"
    	                + "  FROM public.\"Students\" as s,\n"
    	                + "       public.\"Groups\" as g,\n"
    	                + "       public.\"Student_Classes\" as sc\n"
    	                + " WHERE g._id = sc.group_id\n"
    	                + "   AND sc.student_id = s._id\n"
    	                + "   AND sc.class_id = ?\n"
    	                + "   AND sc._id = (SELECT sc2._id\n"
    	                + "                   FROM public.\"Student_Classes\" as sc2\n"
    	                + "                  WHERE sc.student_id = sc2.student_id\n"
    	                + "                 ORDER BY sc2.creation_date DESC\n"
    	                + "                 LIMIT 1) \n"
    	                // Select all closed groups (without students, 
    	                //		because a group with students can not be closed )
    	                + " UNION \n" 
       	                + "SELECT g2._id as group_id,\n"
    	                + "       g2.name as group_name,\n"
    	                + "       g2.open as group_open,\n"
    	                + "       0 as student_id,\n"
    	                + "       '' as username,\n"
    	                + "       '' as gender,\n"
    	                + "       0 as age,\n"
    	                + "		  1 as EmptyStudents\n"
    	                + "  FROM public.\"Groups\" as g2 \n"
    	                + " WHERE (g2.class_id = ?) "
    	                + "	  AND (g2.open = false);";
        	} else {
    			statement = ""
    					// Select all open groups with students 
		                + "SELECT g._id as group_id,\n"
		                + "       g.name as group_name,\n"
		                + "       g.open as group_open,\n"
		                + "       s._id as student_id,\n"
		                + "       s.username,\n"
		                + "       s.gender,\n"
		                + "       s.age,\n"
		                + "		  0 as EmptyStudents\n"
		                + "  FROM public.\"Students\" as s,\n"
		                + "       public.\"Groups\" as g,\n"
		                + "       public.\"Student_Classes\" as sc\n"
		                + " WHERE g._id = sc.group_id\n"
		                + "   AND sc.student_id = s._id\n"
		                + "   AND sc.class_id = ?\n"
		                + "   AND sc._id = (SELECT sc2._id\n"
		                + "                   FROM public.\"Student_Classes\" as sc2\n"
		                + "                  WHERE sc.student_id = sc2.student_id\n"
		                + "                 ORDER BY sc2.creation_date DESC\n"
		                + "                 LIMIT 1)";
        	}
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                if (all) {
                	select.setInt(1, _class_id);
                	select.setInt(2, _class_id);
                } else {
                	select.setInt(1, _class_id);
                };

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        String group_name = result.getString("group_name");
                        int student_id = result.getInt("student_id");
                        String _username = result.getString("username");
                        String _gender = result.getString("gender");
                        int _age = result.getInt("age");
                        boolean group_open = result.getBoolean("group_open");
                        int noStudent = result.getInt("EmptyStudents");
                        // add Student to Group
                        studentGroups.compute(result.getInt("group_id"),
                            (group_id, group) -> {
                                if (group == null) {
                                    group = new StudentGroup(){{
                                        this._id = group_id;
                                        this.name = group_name;
                                        this.open = group_open;
                                        this.class_id = _class_id;
                                    }};
                                }
                                if (noStudent == 0) {
	                                group.students.add(new Student() {{
	                                    this._id = student_id;
	                                    this.username = _username;
	                                    this.gender = _gender;
	                                    this.age = _age;
	                                }});
                                };
                                return group;
                                
                            });
                    }
                    //hotfix TODO refactor
                    // adding missing groups
                    for(Group group : getAllGroupsFromClass(_class_id,all)){
                    	if(!studentGroups.containsKey(group._id)){
                    		StudentGroup gr = new StudentGroup();
                    		gr._id = group._id;
                    		gr.name = group.name;
                    		studentGroups.put(group._id, gr);
                    	}
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(studentGroups);
        return studentGroups.values().stream().collect(Collectors.toList());
    }

    public List<ClassThemeAverage> getClassThemeAverage(int class_id,
            int survey_id) {
        log.traceEntry("Getting average for survey {} in class {}", survey_id, class_id);
        ArrayList<ClassThemeAverage> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "SELECT avg(answer),\"Themes\".title,\"Themes\".title_fi,\"Themes\".description,\"Themes\".description_fi,\"Themes\"._id,\"Surveys\".start_date "
                    + "FROM public.\"Surveys\",public.\"Answers\", public.\"Student_Classes\",public.\"Groups\", public.\"Themes\", public.\"Questions\" "
                    + "WHERE \"Questions\"._id = question_id "
                    + "AND \"Questions\".theme_id = \"Themes\"._id "
                    + "AND \"Answers\".student_id = \"Student_Classes\".student_id "
                    + "AND \"Surveys\"._id = \"Answers\".survey_id "
                    + "AND \"Student_Classes\".group_id = \"Groups\"._id "
                    + "AND \"Groups\".class_id = ? "
                    + "AND \"Answers\".survey_id = ? "
                    + "GROUP BY \"Themes\"._id,start_date";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, class_id);
                select.setInt(2, survey_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        ClassThemeAverage answer = new ClassThemeAverage();
                        //answer.setQuestion_id(result.getInt(1));
                        answer.setAnswer(result.getFloat(1));
                        answer.setTheme_title(result.getString(2));
                        answer.setTheme_title_fi(result.getString(3));
                        answer.setDescription(result.getString(4));
                        answer.setDescription(result.getString(5));
                        answer.setTheme_id(result.getInt(6));
                        answer.setStart_date(result.getString(7));
                        answer.setClass_id(class_id);
                        answer.setSurvey_id(survey_id);
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    }

    public List<StudentThemeAverage> getStudentThemeAverage(int survey_id,
            int student_id) {
        log.traceEntry("Getting average for survey {} in student {}", survey_id, student_id);
        ArrayList<StudentThemeAverage> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "SELECT avg(answer),\"Themes\".title,\"Themes\".title_fi,\"Themes\".description,\"Themes\".description_fi,\"Themes\"._id,\"Surveys\".start_date "
                    + "FROM public.\"Surveys\",public.\"Answers\", public.\"Student_Classes\",public.\"Groups\", public.\"Themes\", public.\"Questions\" "
                    + "WHERE \"Questions\"._id = question_id "
                    + "AND \"Questions\".theme_id = \"Themes\"._id "
                    + "AND \"Answers\".student_id = ? "
                    + "AND \"Surveys\"._id = \"Answers\".survey_id "
                    + "AND \"Answers\".survey_id = ? "
                    + "GROUP BY \"Themes\"._id,start_date";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                select.setInt(1, student_id);
                select.setInt(2, survey_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        StudentThemeAverage answer = new StudentThemeAverage();
                        answer.setAnswer(result.getFloat(1));
                        answer.setTheme_title(result.getString(2));
                        answer.setTheme_title_fi(result.getString(3));
                        answer.setDescription(result.getString(4));
                        answer.setDescription_fi(result.getString(5));
                        answer.setTheme_id(result.getInt(6));
                        answer.setStart_date(result.getString(7));
                        answer.setStudent_id(student_id);
                        answer.setSurvey_id(survey_id);
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    }

    public Optional<List<ListStudentThemeAverage>> getStudentThemeAverageProgression(int student_id, int amount) {
        log.traceEntry("Getting progression of {} for student {}", amount, student_id);
        try {
            List<ListStudentThemeAverage> result = DataBaseHelper.query(Database::getDBConnection, ""
                    + "SELECT * FROM (\n"
                    + "    SELECT\n"
                    + "        DENSE_RANK() OVER(ORDER BY su._id ASC) AS survey_rank,\n"
                    + "        avg(an.answer) as average,\n"
                    + "        su._id as survey_id,\n"
                    + "        su.class_id,\n"
                    + "        su.start_date,\n"
                    + "        su.end_date,\n"
                    + "        su.title as survey_title,\n"
                    + "        su.title_fi as survey_title_fi,\n"
                    + "        su.description as survey_description,\n"
                    + "        su.description_fi as survey_description_fi,\n"
                    + "        su.open as survey_open,\n"
                    + "        su.teacher_id,\n"
                    + "        th.title,\n"
                    + "        th.title_fi,\n"
                    + "        th.description,\n"
                    + "        th.description_fi,\n"
                    + "        th._id as theme_id\n"
                    + "    FROM public.\"Surveys\" as su,\n"
                    + "         public.\"Answers\" as an,\n"
                    + "         public.\"Themes\" as th,\n"
                    + "         public.\"Questions\" as qu \n"
                    + "    WHERE qu._id = an.question_id \n"
                    + "      AND qu.theme_id = th._id \n"
                    + "      AND an.student_id = ? \n"
                    + "      AND su._id = an.survey_id \n"
                    + "    GROUP BY su._id, th._id\n"
                    + "    ORDER BY su.start_date ASC, th._id\n"
                    + ") x WHERE x.survey_rank <= ?",
                    select -> {
                        select.setInt(1, student_id);
                        select.setInt(2, amount);
                    },
                    results -> new ArrayList<ListStudentThemeAverage>() {{
                        int last_survey_rank = -2;
                        for (ResultSet result : results) {
                            StudentThemeAverage answer = new StudentThemeAverage();
                            answer.setAnswer(result.getFloat("average"));
                            answer.setTheme_title(result.getString("title"));
                            answer.setTheme_title_fi(result.getString("title_fi"));
                            answer.setDescription(result.getString("description"));
                            answer.setDescription_fi(result.getString("description_fi"));
                            answer.setTheme_id(result.getInt("theme_id"));
                            answer.setStart_date(result.getString("start_date"));
                            answer.setStudent_id(student_id);
                            answer.setSurvey_id(result.getInt("survey_id"));
                            int survey_rank = result.getInt("survey_rank") - 1;
                            if (last_survey_rank == survey_rank) {
                                this.get(survey_rank).themes.add(answer);
                            } else {
                                last_survey_rank = survey_rank;
                                this.add(new ListStudentThemeAverage() {{
                                    this.themes = Lists.newArrayList(answer);
                                    this.survey = new Survey() {{
                                         
                                    }};
                                }});
                            }
                        }
                    }}
            );
            log.traceExit(result);
            return Optional.of(result);
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No average returned");
            return Optional.empty();
        }
    }

    public Optional<List<ListStudentThemeAverage>> getStudentThemeAverageProgressionInClass(int class_id_,
            int student_id, int amount) {
        log.traceEntry("Getting progression of {} for student {} in class {}", amount, student_id, class_id_);
        try {
            List<ListStudentThemeAverage> result = DataBaseHelper.query(Database::getDBConnection, ""
                    + "SELECT * FROM (\n"
                    + "    SELECT\n"
                    + "        DENSE_RANK() OVER(ORDER BY su._id ASC) AS survey_rank,\n"
                    + "        avg(an.answer) as average,\n"
                    + "        su._id as survey_id,\n"
                    + "        su.start_date,\n"
                    + "        su.end_date,\n"
                    + "        su.title as survey_title,\n"
                    + "        su.title as survey_title_fi,\n"
                    + "        su.description as survey_description,\n"
                    + "        su.description as survey_description_fi,\n"
                    + "        su.open as survey_open,\n"
                    + "        su.teacher_id,\n"
                    + "        th.title,\n"
                    + "        th.title_fi,\n"
                    + "        th.description,\n"
                    + "        th.description_fi,\n"
                    + "        th._id as theme_id\n"
                    + "    FROM public.\"Surveys\" as su,\n"
                    + "         public.\"Answers\" as an,\n"
                    + "         public.\"Student_Classes\" as sc,\n"
                    + "         public.\"Themes\" as th,\n"
                    + "         public.\"Questions\" as qu \n"
                    + "    WHERE qu._id = an.question_id \n"
                    + "      AND qu.theme_id = th._id \n"
                    + "      AND an.student_id = ? \n"
                    + "      AND su._id = an.survey_id \n"
                    + "      AND sc.student_id = an.student_id\n"
                    + "      AND sc.class_id = ?\n"
                    + "    GROUP BY su._id, th._id\n"
                    + "    ORDER BY su.start_date ASC, th._id\n"
                    + ") x WHERE x.survey_rank <= ?",
                    select -> {
                        select.setInt(1, student_id);
                        select.setInt(2, class_id_);
                        select.setInt(3, amount);
                    },
                    results -> new ArrayList<ListStudentThemeAverage>() {{
                        int last_survey_rank = -2;
                        for (ResultSet result : results) {
                            StudentThemeAverage answer = new StudentThemeAverage();
                            answer.setAnswer(result.getFloat("average"));
                            answer.setTheme_title(result.getString("title"));
                            answer.setTheme_title_fi(result.getString("title_fi"));
                            answer.setDescription(result.getString("description"));
                            answer.setDescription_fi(result.getString("description_fi"));
                            answer.setTheme_id(result.getInt("theme_id"));
                            answer.setStart_date(result.getString("start_date"));
                            answer.setStudent_id(student_id);
                            answer.setSurvey_id(result.getInt("survey_id"));
                            int survey_rank = result.getInt("survey_rank") - 1;
                            if (last_survey_rank == survey_rank) {
                                this.get(survey_rank).themes.add(answer);
                            } else {
                                last_survey_rank = survey_rank;
                                this.add(new ListStudentThemeAverage() {{
                                    this.themes = Lists.newArrayList(answer);
                                    this.survey = new Survey() {{
                                        this._id = result.getInt("survey_id");
                                        this.class_id = class_id_;
                                        this.description = result.getString("survey_description");
                                        this.start_date = result.getTimestamp("start_date");
                                        this.end_date = result.getTimestamp("end_date");
                                        this.open = result.getBoolean("survey_open");
                                        this.teacher_id = result.getInt("teacher_id");
                                        this.title = result.getString("survey_title");
                                    }};
                                }});
                            }
                        }
                    }}
            );
            log.traceExit(result);
            return Optional.of(result);
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No average returned");
            return Optional.empty();
        }
    }

    public Optional<List<ListGroupThemeAverage>> getGroupThemeAverageProgression(int class_id_,
            int group_id, int amount) {
        log.traceEntry("Getting progression of {} for group {} in class {}", amount, group_id, class_id_);
        try {
            List<ListGroupThemeAverage> result = DataBaseHelper.query(Database::getDBConnection, ""
                            + "SELECT \n"
                            + "        DENSE_RANK() OVER(ORDER BY su._id ASC) AS survey_rank,\n"
                            + "        avg(an.answer) as average,\n"
                            + "        su._id as survey_id,\n"
                            + "        su.class_id,\n"
                            + "        su.start_date,\n"
                            + "        su.end_date,\n"
                            + "        su.title as survey_title,\n"
                            + "        su.title_fi as survey_title_fi,\n"
                            + "        su.description as survey_description,\n"
                            + "        su.description_fi as survey_description_fi,\n"
                            + "        su.open as survey_open,\n"
                            + "        su.teacher_id,\n" 
                            + "        th.title,\n"
                            + "        th.title_fi,\n"
                            + "        th.description,\n"
                            + "        th.description_fi,\n"
                            + "        th._id as theme_id\n"
                            + "FROM public.\"Surveys\" AS su\n"
                            + "FULL JOIN public.\"Answers\" AS an ON an.survey_id=su._id\n"
                            + "FULL JOIN public.\"Questions\" AS qu ON qu._id=an.question_id\n"
                            + "INNER JOIN public.\"Themes\" AS th ON th._id=qu.theme_id\n"
                            + "WHERE su.open = false and su.class_id = ? and an.group_id = ?\n"
                            + "GROUP BY su._id,th._id,an.group_id ORDER BY su.start_date, th._id",
                    select -> {
                        select.setInt(1, class_id_);
                        select.setInt(2, group_id);
                   //     select.setInt(3, amount);
                    },

                    results -> new ArrayList<ListGroupThemeAverage>() {{
                        int last_survey_rank = -2;
                        for (ResultSet result : results) {
                            GroupThemeAverage answer = new GroupThemeAverage();
                            answer.setAnswer(result.getFloat("average"));
                            answer.setTheme_title(result.getString("title"));
                            answer.setTheme_title_fi(result.getString("title_fi"));
                            answer.setDescription(result.getString("description"));
                            answer.setDescription_fi(result.getString("description_fi"));
                            answer.setTheme_id(result.getInt("theme_id"));
                            answer.setStart_date(result.getString("start_date"));
                            answer.setGroup_id(group_id);
                            answer.setSurvey_id(result.getInt("survey_id"));
                            int survey_rank = result.getInt("survey_rank") - 1;
                            if (last_survey_rank == survey_rank) {
                                this.get(survey_rank).themes.add(answer);
                            } else {
                                last_survey_rank = survey_rank;
                                this.add(new ListGroupThemeAverage() {{
                                    this.themes = Lists.newArrayList(answer);
                                    this.survey = new Survey() {{
                                        this._id = result.getInt("survey_id");
                                        this.class_id = class_id_;
                                        this.description = result.getString("survey_description");
                                        this.description_fi = result.getString("survey_description_fi");
                                        this.start_date = result.getTimestamp("start_date");
                                        this.end_date = result.getTimestamp("end_date");
                                        this.open = result.getBoolean("survey_open");
                                        this.teacher_id = result.getInt("teacher_id");
                                        this.title = result.getString("survey_title");
                                        this.title_fi = result.getString("survey_title_fi");
                                    }};
                                }});
                            }
                        }
                    }}
            );
            log.traceExit(result);
            return Optional.of(result);
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No average returned");
            return Optional.empty();
        }
    }
    
    //Is this used by the front end at all?
    public Optional<List<ListClassThemeAverage>> getClassThemeAverageProgression(int class_id_, int amount) {
        log.traceEntry("Getting progression of {} for class {}", amount, class_id_);
        try {
            List<ListClassThemeAverage> result = DataBaseHelper.query(Database::getDBConnection, ""
                    + "SELECT * FROM (\n"
                    + "    SELECT\n"
                    + "        DENSE_RANK() OVER(ORDER BY su._id ASC) AS survey_rank,\n"
                    + "        avg(an.answer) as average,\n"
                    + "        su._id as survey_id,\n"
                    + "        su.class_id,\n"
                    + "        su.start_date,\n"
                    + "        su.end_date,\n"
                    + "        su.title as survey_title,\n"
                    + "        su.title_fi as survey_title_fi,\n"
                    + "        su.description as survey_description,\n"
                    + "        su.description_fi as survey_description_fi,\n"
                    + "        su.open as survey_open,\n"
                    + "        su.teacher_id,\n"
                    + "        th.title,\n"
                    + "        th.title_fi,\n"
                    + "        th.description,\n"
                    + "        th.description_fi,\n"
                    + "        th._id as theme_id\n"
                    + "    FROM public.\"Surveys\" as su,\n"
                    + "         public.\"Answers\" as an,\n"
                    + "	        public.\"Student_Classes\" as sc,\n"
                    + "         public.\"Themes\" as th,\n"
                    + "	        public.\"Questions\" as qu \n"
                    + "    WHERE qu._id = an.question_id \n"
                    + "      AND qu.theme_id = th._id \n"
                    + "      AND su._id = an.survey_id \n"
                    + "      AND sc.student_id = an.student_id\n"
                    + "      AND sc.class_id = ?\n"
                    + "    GROUP BY su._id, th._id\n"
                    + "    ORDER BY su.start_date ASC, th._id\n"
                    + ") x WHERE x.survey_rank <= ?",
                    select -> {
                        select.setInt(1, class_id_);
                        select.setInt(2, amount);
                    },
                    results -> new ArrayList<ListClassThemeAverage>() {{
                        int last_survey_rank = -2;
                        for (ResultSet result : results) {
                            ClassThemeAverage answer = new ClassThemeAverage();
                            answer.setAnswer(result.getFloat("average"));
                            answer.setTheme_title(result.getString("title"));
                            answer.setTheme_title_fi(result.getString("title_fi"));
                            answer.setDescription(result.getString("description"));
                            answer.setDescription_fi(result.getString("description_fi"));
                            answer.setTheme_id(result.getInt("theme_id"));
                            answer.setStart_date(result.getString("start_date"));
                            answer.setClass_id(class_id_);
                            answer.setSurvey_id(result.getInt("survey_id"));
                            int survey_rank = result.getInt("survey_rank") - 1;
                            if (last_survey_rank == survey_rank) {
                                this.get(survey_rank).themes.add(answer);
                            } else {
                                last_survey_rank = survey_rank;
                                this.add(new ListClassThemeAverage() {{
                                    this.themes = Lists.newArrayList(answer);
                                    this.survey = new Survey() {{
                                        this._id = result.getInt("survey_id");
                                        this.class_id = class_id_;
                                        this.description = result.getString("survey_description");
                                        this.description_fi = result.getString("survey_description_fi");
                                        this.start_date = result.getTimestamp("start_date");
                                        this.end_date = result.getTimestamp("end_date");
                                        this.open = result.getBoolean("survey_open");
                                        this.teacher_id = result.getInt("teacher_id");
                                        this.title = result.getString("survey_title");
                                        this.title_fi = result.getString("survey_title_fi");
                                    }};
                                }});
                            }
                        }
                    }});
            log.traceExit(result);
            return Optional.of(result);
        } catch (SQLException e) {
            log.catching(e);
            log.traceExit("No average returned");
            return Optional.empty();
        }
    }

    public void closeSurvey(int teacher_id, int class_id, int survey_id) {
        log.traceEntry("Closing survey {} for teacher {} in class {}", survey_id, teacher_id, class_id);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "UPDATE public.\"Surveys\" "
                    + "SET (open,end_date) = (false,now())"
                    + "WHERE teacher_id = ? " + "AND _id = ? "
                    + "AND class_id = ?";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setInt(1, teacher_id);
                insert.setInt(2, survey_id);
                insert.setInt(3, class_id);
                // execute query
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit();
    }
    
    public List<StudentThemeAverage> getStudentLifeTimeAverage(int student_id, int class_id){
        log.traceEntry("Getting student {} average in class {}", student_id, class_id);
    	ArrayList<StudentThemeAverage> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String select_averages = "SELECT avg(answer),\"Themes\".title,\"Themes\".title_fi,\"Themes\".description,\"Themes\".description_fi,\"Themes\"._id,\"Surveys\".start_date,\"Surveys\"._id,\"Surveys\".title,\"Surveys\".title_fi,\"Surveys\".description,\"Surveys\".description_fi,\"Surveys\".start_date,\"Surveys\".end_date "
            		+ "FROM public.\"Surveys\",public.\"Answers\", public.\"Student_Classes\",public.\"Groups\", public.\"Themes\", public.\"Questions\" "
            		+ "WHERE \"Questions\"._id = question_id "
            		+ "AND \"Questions\".theme_id = \"Themes\"._id "
            		+ "AND \"Answers\".student_id = ? "
            		+ "AND \"Groups\".class_id = ?"
            		+ "AND \"Surveys\"._id = \"Answers\".survey_id "
            		+ "GROUP BY \"Surveys\"._id,\"Themes\"._id,start_date "
            		+ "ORDER BY start_date DESC";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(select_averages)) {
                select.setInt(1, student_id);
                select.setInt(2, class_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        StudentThemeAverage answer = new StudentThemeAverage();
                        answer.setAnswer(result.getFloat(1));
                        answer.setTheme_title(result.getString(2));
                        answer.setTheme_title_fi(result.getString(3));
                        answer.setDescription(result.getString(4));
                        answer.setDescription_fi(result.getString(5));
                        answer.setTheme_id(result.getInt(6));
                        answer.setStart_date(result.getString(7));
                        answer.setStudent_id(student_id);
                        answer.setSurvey_id(-1);
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    }

    public List<StudentThemeAverage> getStudentHistory(int student_id, int class_id){
        log.traceEntry("Getting student {} history in class {}", student_id, class_id);
    	ArrayList<StudentThemeAverage> answers = new ArrayList<>();
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String select_averages = "SELECT avg(answer),\"Themes\".title,\"Themes\".title_fi,\"Themes\".description,\"Themes\".description_fi,\"Themes\"._id,\"Surveys\".start_date,\"Surveys\"._id,\"Surveys\".title,\"Surveys\".title_fi,\"Surveys\".description,\"Surveys\".description_fi,\"Surveys\".start_date,\"Surveys\".end_date "
            		+ "FROM public.\"Surveys\",public.\"Answers\", public.\"Student_Classes\",public.\"Groups\", public.\"Themes\", public.\"Questions\" "
            		+ "WHERE \"Questions\"._id = question_id "
            		+ "AND \"Questions\".theme_id = \"Themes\"._id "
            		+ "AND \"Answers\".student_id = ? "
            		+ "AND \"Groups\".class_id = ?"
            		+ "AND \"Surveys\"._id = \"Answers\".survey_id "
            		+ "GROUP BY \"Surveys\"._id,\"Themes\"._id,start_date "
            		+ "ORDER BY start_date DESC";
            //prepare statement with survey_id
            try (PreparedStatement select = dbConnection
                    .prepareStatement(select_averages)) {
                select.setInt(1, student_id);
                select.setInt(2, class_id);

                // execute query
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        StudentThemeAverage answer = new StudentThemeAverage();
                        answer.setAnswer(result.getFloat(1));
                        answer.setTheme_title(result.getString(2));
                        answer.setTheme_title_fi(result.getString(3));
                        answer.setDescription(result.getString(4));
                        answer.setDescription_fi(result.getString(5));
                        answer.setTheme_id(result.getInt(6));
                        answer.setStart_date(result.getString(7));
                        answer.setStudent_id(student_id);
                        answers.add(answer);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(answers);
        return answers;
    	
    }
    
    public boolean doesGroupClassMatch(int group_id, int class_id) {
        try (Connection dbConnection = getDBConnection()) {
            return DataBaseHelper.doesGroupClassMatch(dbConnection, group_id, class_id);
        } catch (SQLException e) {
            log.catching(e);
            return false;
        }
    }

    public boolean isGroupClosed(int group_id) {
        log.traceEntry("Checking if group {} is closed", group_id);
        // If group is closed, it is empty.
        boolean closed = false;
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String select_open = "SELECT open "
                    + "FROM public.\"Groups\" "
                    + "WHERE _id = ?; ";
            try (PreparedStatement select = dbConnection
                    .prepareStatement(select_open)) {
                select.setInt(1, group_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        closed = result.getBoolean("open");
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(closed);
        return closed;
    }

    public int countNumberOfStudentsInGroup(int group_id) {
        log.traceEntry("Counting how many students are in group {}", group_id);
        int count = 0;
        try (Connection dbConnection = getDBConnection()) {
            String select_students_count = "SELECT COUNT(t1.group_id) "
                    + "FROM public.\"Student_Classes\" AS t1 "
                    + "INNER JOIN ("
                    + "		SELECT student_id, class_id, MAX(creation_date) as maxdate "
                    + "		FROM public.\"Student_Classes\" "
                    + "		GROUP BY student_id, class_id"
                    + ") AS t2 "
                    + "ON t1.student_id = t2.student_id "
                    + "	AND t1.class_id = t2.class_id "
                    + "	AND t1.creation_date = t2.maxdate "
                    + "WHERE t1.group_id = ?;";
            try (PreparedStatement select = dbConnection
                    .prepareStatement(select_students_count)) {
                select.setInt(1, group_id);
                // execute query
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        count = result.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(count);
        return count;
    }

    // TODO: Use optional
    public Group createGroupInClass(int class_id, Group group) {
        log.traceEntry("Creating group {} in class {}", group, class_id);
        Group createdGroup = null;
        try (Connection dbConnection = getDBConnection()) {
            String insert_group = "INSERT INTO public.\"Groups\" "
                    + "(name, class_id) VALUES (?, ?) RETURNING _id;";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(insert_group)) {
                insert.setString(1, group.getName());
                insert.setInt(2, class_id);
                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                        createdGroup = new Group();
                        createdGroup.set_id(result.getInt("_id"));
                        createdGroup.setClass_id(class_id);
                        createdGroup.setName(group.getName());
                        createdGroup.setOpen(true);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(createdGroup);
        return createdGroup;
    }
    
    public Group createGroupInClass(int class_id, String name) {
        log.traceEntry("Creating group {} in class {}", name, class_id);
        Group createdGroup = null;
        try (Connection dbConnection = getDBConnection()) {
            String insert_group = "INSERT INTO public.\"Groups\" "
                    + "(name, class_id) VALUES (?, ?) RETURNING _id;";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(insert_group)) {
                insert.setString(1, name);
                insert.setInt(2, class_id);
                // execute query
                try (ResultSet result = insert.executeQuery()) {
                    if (result.next()) {
                        createdGroup = new Group();
                        createdGroup.set_id(result.getInt("_id"));
                        createdGroup.setClass_id(class_id);
                        createdGroup.setName(name);
                        createdGroup.setOpen(true);
                    }
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(createdGroup);
        return createdGroup;
    }
    
    public void addClassToTeacher(Classes teacher_class) {
        log.traceEntry("Add class to teacher {}", teacher_class);
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
            String statement = "INSERT INTO public.\"Classes\" (\"name\", \"name_fi\", teacher_id) VALUES (?, ?, ?);";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                insert.setString(1, teacher_class.getName());
                insert.setString(2, teacher_class.getName_fi());
                insert.setInt(3, teacher_class.getTeacher_id());
                // execute query
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit();
    }

    public DataSource getDataSource() {
        return DATA_SOURCE;
    }


	public List<Student> getAllStudents() {//(int teacher_id)  
        log.traceEntry("Getting all students ");//from {}", teacher_id);
        List<Student> students = null;
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
    		String statement = "" 
		                    + "SELECT s._id,\n"
		                    + "       s.username,\n"
		                    + "       s.gender,\n"
		                    + "       s.age\n"
		                    + "  FROM public.\"Students\" as s\n";
		                    //+ " WHERE s._id = (SELECT sc2.student_id\n"
		                    //+ "           	   FROM public.\"Student_Classes\" as sc2\n"
		                    //+ "                INNER JOIN public.\"Classes\" as cls\n"
		                    //+ "                ON (cls.teacher_id = ?)\n"
		                    //+ "				    AND (cls._id = sc2.class_id)\n"
		                    //+ "                WHERE s._id = sc2.student_id\n"
		                    //+ "              	ORDER BY sc2.creation_date DESC\n"
		                    //+ "            	 	LIMIT 1) \n";
            try (PreparedStatement insert = dbConnection
                    .prepareStatement(statement)) {
                // execute query
            	students = new ArrayList<Student>();
                //insert.setInt(1, teacher_id);
                try (ResultSet result = insert.executeQuery()) {
                    while (result.next()) {
                        Student student = new Student(result.getInt("_id"),
                        							  result.getString("username"),
                        							  result.getString("gender"),
                        							  result.getInt("age"));  
                        students.add(student);
                    };
                };
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(students);
        return students;
	}

	public boolean doesStudentIdExistInDatabase(int student_id)	{
		boolean idExists = false;
        log.traceEntry("Checking whether a student with student_id = {}", student_id, " exists in databsae.");
        try (Connection dbConnection = getDBConnection()) {
            // Set up batch of statements
    		String statement = "" 
		                    + "SELECT COUNT(s._id)\n"
		                    + "  FROM public.\"Students\" as s\n"
		                    + " WHERE s._id = ?\n";
            try (PreparedStatement select = dbConnection
                    .prepareStatement(statement)) {
                // execute query
                select.setInt(1, student_id);
                try (ResultSet result = select.executeQuery()) {
                    if (result.next() & (result.getInt(1) != 0)) {
                    	idExists = true;
                    };
                };
            }
        } catch (SQLException e) {
            log.catching(e);
        }
        log.traceExit(idExists);
        return idExists;
	}
}
