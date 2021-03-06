package org.dlearn.helsinki.skeleton.service;

import java.util.List;

import org.dlearn.helsinki.skeleton.database.Database;
import org.dlearn.helsinki.skeleton.model.Answer;
import org.dlearn.helsinki.skeleton.model.ThemeAverage;

public class StudentSurveyAnswerService {

    Database db = new Database();

    public void putAnswerToQuestion(int student_id, int survey_id,
            int question_id, Answer answer, int class_id) {
        answer.question_id = question_id;
        answer.student_id = student_id;
        answer.survey_id = survey_id;
        db.putAnswerToQuestion(answer, class_id);
    }

    public List<Answer> getAnswers(int student_id, int survey_id) {
        return db.getAnswersFromStudentSurvey(student_id, survey_id);
    }

    public List<ThemeAverage> getStudentThemeAverage(int student_id,
            int survey_id) {
        return db.getSurveyAnswerAverages(student_id, 0, 0, survey_id);
    }

    public boolean postAnswersToQuestion(int class_id, int survey_id,
            int student_id, List<Answer> answers) {
        return db.postStudentAnswersForSurvey(class_id, survey_id, student_id,
                answers);
    }

}
