package com.example.connectbase;

class Friend {

    private String age;
    private String city;
    private String email;
    private String experience;
    private String image;
    private String mobile;
    private String name;
    private String organisation;
    private String position;
    private String qualification;
    private String resume;
    private String skills;
    private String state;
    private String thumbImage;

    public Friend() {
    }

    Friend(String age, String city, String email, String experience, String image, String mobile, String name, String organisation, String position, String qualification, String resume, String skills, String state, String thumbImage) {
        this.age = age;
        this.city = city;
        this.email = email;
        this.experience = experience;
        this.image = image;
        this.mobile = mobile;
        this.name = name;
        this.organisation = organisation;
        this.position = position;
        this.qualification = qualification;
        this.resume = resume;
        this.skills = skills;
        this.state = state;
        this.thumbImage = thumbImage;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getThumbImage() {
        return thumbImage;
    }

    public void setThumbImage(String thumbImage) {
        this.thumbImage = thumbImage;
    }
}
