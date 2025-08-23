package edu.ezip.ing1.pds.business.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private Long id;
    private String email;
    private String password;
    private String fullName;
    private Double heightCm;
    private Double weightKg;
    private Integer age;
    private String activityLevel;
    private String sex;
    private Integer dailyKcalTarget;
    private Double bmi;

    public User() {}

    public void computeBmi() {
        if (heightCm != null && heightCm > 0 && weightKg != null && weightKg > 0) {
            double h = heightCm / 100.0;
            this.bmi = Math.round((weightKg / (h*h)) * 100.0) / 100.0;
        }
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public Integer getDailyKcalTarget() { return dailyKcalTarget; }
    public void setDailyKcalTarget(Integer dailyKcalTarget) { this.dailyKcalTarget = dailyKcalTarget; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }
}
