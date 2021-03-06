package org.dlearn.helsinki.skeleton.service;

import java.util.List;
import java.util.Optional;
import org.dlearn.helsinki.skeleton.database.Database;
import org.dlearn.helsinki.skeleton.model.Group;
import org.dlearn.helsinki.skeleton.model.Student;
import org.dlearn.helsinki.skeleton.model.ThemeAverage;

public class StudentService {

    static final Database db = new Database();

    //TODO Student now does not contain password, so this service is redundant right now. It does the same as TeacherStudentService.
    public Student getStudent(int student_id) {
        return db.getStudent(student_id);
    }

    /**
     * used for every average query, returns themes with average valued answers
     * @param student_id
     * @param class_id
     * @param group_id
     * @param survey_id
     * @return 
     */
    public List<ThemeAverage> getSurveyAnswerAverages(int student_id,
            int class_id, int group_id, int survey_id) {
        return db.getSurveyAnswerAverages(student_id, class_id, group_id,
                survey_id);
    }

    /**
     * used to get groups average answers values for single survey
     * @param student_id
     * @param class_id
     * @param survey_id
     * @return 
     */
    public List<ThemeAverage> getGroupSurveyAnswerAverages(int student_id,
            int class_id, int survey_id) {
        Optional<Group> grp = db.getGroupForStudent(class_id, student_id);
        if (grp.isPresent()) {
            return db.getSurveyAnswerAverages(0, class_id, grp.get()._id,
                    survey_id);
        } else {
            return null;
        }
    }

    /**
     * used to get groups average answers values for all the surveys
     * @param student_id
     * @param class_id
     * @return 
     */
    public List<ThemeAverage> getGroupAnswerAverages(int student_id,
            int class_id) {
        Optional<Group> grp = db.getGroupForStudent(class_id, student_id);
        if (grp.isPresent()) {
            return db.getSurveyAnswerAverages(0, class_id, grp.get()._id, 0);
        } else {
            return null;
        }
    }
}
